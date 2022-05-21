package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerGalaxyAtWar
import com.jacobtread.kme.database.PlayerGalaxyAtWars
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.min


fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val port = config.ports.http
        val handler = HTTPHandler(config)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline()
                        .addLast(HttpRequestDecoder())
                        .addLast(HttpResponseEncoder())
                        .addLast(handler)
                }
            })
            .bind(port)
            .addListener { Logger.info("Started HTTP Server on port $port") }
    } catch (e: IOException) {
        Logger.error("Exception in HTTP server", e)
    }
}

@Sharable
private class HTTPHandler(private val config: Config) : SimpleChannelInboundHandler<HttpRequest>() {

    companion object {
        private val XML_PRINT_OPTIONS = PrintOptions(singleLineTextElements = true, pretty = false)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val method = msg.method()
        if (method != HttpMethod.GET) {
            ctx.respond(HttpResponseStatus.NOT_FOUND)
            return
        }
        val url = msg.uri()
        Logger.debug("HTTP Request: $url")
        if (url.startsWith("/wal/masseffect-gaw-pc")) {
            gawResponse(ctx, url)
        } else {
            fileSystemResponse(ctx, url)
        }
    }

    fun fileSystemResponse(ctx: ChannelHandlerContext, url: String) {
        val fileName = url.substringAfterLast('/')
        Logger.debug("HTTP Request for: $fileName")
        val pathName = "/public/$fileName"
        val inputStream = HTTPHandler::class.java.getResourceAsStream(pathName);
        if (inputStream == null) {
            ctx.respond(HttpResponseStatus.NOT_FOUND)
        } else {
            val contents = inputStream.readAllBytes()
            ctx.respond(
                contents,
                headers = mapOf(
                    "Accept-Ranges" to "bytes",
                    "ETag" to "524416-1333666807000"
                )
            )
        }
    }

    private fun ChannelHandlerContext.respond(
        content: Any? = null,
        status: HttpResponseStatus = HttpResponseStatus.OK,
        contentType: String? = null,
        headers: Map<String, String>? = null,
    ) {
        if (content != null) {
            val type: String
            val contentBuffer = when (content) {
                is String -> {
                    type = contentType ?: "text;charset=UTF-8"
                    Unpooled.copiedBuffer(content, Charsets.UTF_8)
                }
                is ByteArray -> {
                    type = contentType ?: ""
                    Unpooled.wrappedBuffer(content)
                }
                is Node -> {
                    type = contentType ?: "text/xml;charset=UTF-8"
                    val encoded = content.toString(XML_PRINT_OPTIONS)
                    Unpooled.copiedBuffer(encoded, Charsets.UTF_8)
                }
                else -> throw IllegalArgumentException("Dont know how to handle unknown content type: $content")
            }
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, contentBuffer)
            val headersOut = response.headers()
            if (type.isNotEmpty()) headersOut.add("Content-Type", type)
            headersOut.add("Content-Length", contentBuffer.readableBytes())
            headers?.forEach { (key, value) -> headersOut.add(key, value) }
            writeAndFlush(response)
        } else {
            writeAndFlush(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status))
        }
    }


    private fun parseQuery(value: String): Map<String, String> {
        val out = HashMap<String, String>()
        value.split('&').forEach { keyValue ->
            val parts = keyValue.split('=', limit = 2)
            if (parts.size > 1) {
                out[URLDecoder.decode(parts[0], Charsets.UTF_8)] = URLDecoder.decode(parts[1], Charsets.UTF_8)
            } else {
                out[URLDecoder.decode(parts[0], Charsets.UTF_8)] = ""
            }
        }
        return out
    }

    data class Request(val path: List<String>, val query: Map<String, String>)

    fun gawResponse(ctx: ChannelHandlerContext, url: String) {
        val rawPath = url.substring(23)
        if (rawPath.isEmpty()) {
            return ctx.respond(HttpResponseStatus.NOT_FOUND)
        }
        val urlParts = rawPath.split('?', limit = 2)

        val path = urlParts[0].split('/')
        val query = if (urlParts.size > 1) parseQuery(urlParts[1]) else emptyMap()


        if (path.isEmpty()) {
            return ctx.respond(HttpResponseStatus.NOT_FOUND)
        }

        val request = Request(path, query)

        when (path[0]) {
            "authentication" -> handleGalaxyAtWarAuthentication(ctx, request)
            "galaxyatwar" -> when (path[1]) {
                "getRatings" -> handleGalaxyAtWarRatings(ctx, request)
                "increaseRatings" -> handleGalaxyAtWarIncreaseRatings(ctx, request)
                else -> ctx.respond(HttpResponseStatus.BAD_REQUEST)
            }
            else -> ctx.respond(HttpResponseStatus.BAD_REQUEST)
        }
    }

    private fun handleGalaxyAtWarAuthentication(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 2 || !request.path[1].startsWith("sharedTokenLogin")) {
            return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        }
        val playerId = request.query["auth"]?.toIntOrNull(16) ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val player = transaction { Player.findById(playerId) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        Logger.debug("Authenticated GAW User ${player.displayName}")

        val playerIdStr = playerId.toString()
        val time = unixTimeSeconds().toString()

        @Suppress("SpellCheckingInspection")
        ctx.respond(xml("fulllogin") {
            globalProcessingInstruction(
                "xml",
                "version" to "1.0",
                "encoding" to "UTF-8"
            )
            element("canageup", "0")
            element("legaldochost")
            element("needslegaldoc", "0")
            element("pclogintoken", player.sessionToken)
            element("privacypolicyuri")
            "sessioninfo" {
                element("blazeuserid", playerIdStr)
                element("isfirstlogin", "0")
                element("sessionkey", playerIdStr)
                element("lastlogindatetime", time)
                element("email", player.email)
                "personadetails" {
                    element("displayname", player.displayName)
                    element("lastauthenticated", time)
                    element("personaid", playerIdStr)
                    element("status", "UNKNOWN")
                    element("extid", "0")
                    element("exttype", "BLAZE_EXTERNAL_REF_TYPE_UNKNOWN")
                }
                element("userid", playerIdStr)
            }
            element("isoflegalcontactage", "0")
            element("toshost")
            element("termsofserviceuri")
            element("tosuri")
        })
    }

    private fun handleGalaxyAtWarRatings(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 3) {
            return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        }
        val playerId = request.path[2].toIntOrNull() ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val player = transaction { Player.findById(playerId) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val rating = player.getOrCreateGAW(config.gaw)
        respondGAWRating(ctx, player, rating)
    }

    private fun respondGAWRating(ctx: ChannelHandlerContext, player: Player, rating: PlayerGalaxyAtWar) {
        val level = (rating.a + rating.b + rating.c + rating.d + rating.e) / 5
        val promotions = if (config.gaw.enablePromotions) player.getTotalPromotions() else 0

        @Suppress("SpellCheckingInspection")
        ctx.respond(xml("galaxyatwargetratings") {
            globalProcessingInstruction(
                "xml",
                "version" to "1.0",
                "encoding" to "UTF-8"
            )
            "ratings" {
                element("rating", rating.a.toString())
                element("rating", rating.b.toString())
                element("rating", rating.c.toString())
                element("rating", rating.d.toString())
                element("rating", rating.e.toString())
            }
            element("level", level.toString())
            "assets" {
                element("assets", promotions.toString())
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
                element("assets", "0")
            }
        })
    }

    private fun handleGalaxyAtWarIncreaseRatings(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 3) {
            return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        }
        val playerId = request.path[2].toIntOrNull() ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val player = transaction { Player.findById(playerId) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val rating = player.getOrCreateGAW(config.gaw)
        val maxValue = 10099

        transaction {
            rating.apply {
                timestamp = unixTimeSeconds()
                a = min(maxValue, a + (request.query["rinc|0"]?.toIntOrNull() ?: 0))
                b = min(maxValue, b + (request.query["rinc|1"]?.toIntOrNull() ?: 0))
                c = min(maxValue, c + (request.query["rinc|2"]?.toIntOrNull() ?: 0))
                d = min(maxValue, d + (request.query["rinc|3"]?.toIntOrNull() ?: 0))
                e = min(maxValue, e + (request.query["rinc|4"]?.toIntOrNull() ?: 0))
            }
        }

        respondGAWRating(ctx, player, rating)

    }
}