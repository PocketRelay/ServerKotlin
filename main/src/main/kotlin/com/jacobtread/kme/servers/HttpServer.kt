package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min


fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val port = config.ports.http
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline()
                        .addLast(HttpRequestDecoder())
                        .addLast(HttpResponseEncoder())
                        .addLast(HTTPHandler(config))
                }
            })
            .bind(port)
            .addListener { Logger.info("Started HTTP Server on port $port") }
    } catch (e: IOException) {
        Logger.error("Exception in HTTP server", e)
    }
}

private class HTTPHandler(private val config: Config) : SimpleChannelInboundHandler<HttpRequest>() {

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val method = msg.method()
        if (method != HttpMethod.GET) {
            ctx.notFound()
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
            ctx.notFound()
        } else {
            val contents = inputStream.readAllBytes()
            val content = Unpooled.wrappedBuffer(contents)
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content)
            val headers = response.headers()
            headers.add("Accept-Ranges", "bytes")
            headers.add("ETag", "524416-1333666807000")
            headers.add("Content-Length", content.readableBytes())
            ctx.writeAndFlush(response)
        }
    }

    private fun ChannelHandlerContext.notFound() {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
        writeAndFlush(response)
    }

    private fun ChannelHandlerContext.badRequest() {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
        writeAndFlush(response)
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
        if (rawPath.isEmpty()) ctx.notFound()
        val urlParts = rawPath.split('?', limit = 2)

        val path = urlParts[0].split('/')
        val query = if (urlParts.size > 1) parseQuery(urlParts[1]) else emptyMap()


        if (path.isEmpty()) {
            ctx.notFound()
            return
        }

        val request = Request(path, query)

        when (path[0]) {
            "authentication" -> handleGalaxyAtWarAuthentication(ctx, request)
            "galaxyatwar" -> when(path[1]) {
                "getRatings" -> handleGalaxyAtWarRatings(ctx, request)
                "increaseRatings" -> handleGalaxyAtWarIncreaseRatings(ctx, request)
                else -> ctx.badRequest()
            }
            else -> ctx.badRequest()
        }
    }

    private fun handleGalaxyAtWarAuthentication(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 2 || !request.path[1].startsWith("sharedTokenLogin")) {
            ctx.badRequest()
            return
        }
        val playerId = request.query["auth"]?.toIntOrNull()
        if (playerId == null) {
            ctx.badRequest()
            return
        }
        val player = transaction { Player.findById(playerId) }
        if (player == null) {
            ctx.badRequest()
            return
        }
        @Suppress("SpellCheckingInspection")
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <fulllogin>
                <canageup>0</canageup>
                <legaldochost/>
                <needslegaldoc>0</needslegaldoc>
                <pclogintoken>${player.sessionToken}</pclogintoken>
                <privacypolicyuri/>
                <sessioninfo>
                    <blazeuserid>${player.id.value}</blazeuserid>
                    <isfirstlogin>0</isfirstlogin>
                    <sessionkey>${player.id.value}</sessionkey>
                    <lastlogindatetime>1422639771</lastlogindatetime>
                    <email>${player.email}</email>
                    <personadetails>
                        <displayname>${player.displayName}</displayname>
                        <lastauthenticated>1422639540</lastauthenticated>
                        <personaid>${player.id.value}</personaid>
                        <status>UNKNOWN</status>
                        <extid>0</extid>
                        <exttype>BLAZE_EXTERNAL_REF_TYPE_UNKNOWN</exttype>
                    </personadetails>
                    <userid>${player.id.value}</userid>
                </sessioninfo>
                <isoflegalcontactage>0</isoflegalcontactage>
                <toshost/>
                <termsofserviceuri/>
                <tosuri/>
            </fulllogin>
        """.trimIndent()
        val contentBuffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, contentBuffer)
        val headers = response.headers()
        headers.add("Content-Type", "text/xml;charset=UTF-8")
        headers.add("Content-Length", contentBuffer.readableBytes())
        ctx.writeAndFlush(response)
    }

    private fun handleGalaxyAtWarRatings(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 3) {
            ctx.badRequest()
            return
        }
        val playerId = request.path[2].toIntOrNull()
        if (playerId == null) {
            ctx.badRequest()
            return
        }
        val player = transaction { Player.findById(playerId) }
        if (player == null) {
            ctx.badRequest()
            return
        }
        val promotions = if (config.gaw.enablePromotions) player.getTotalPromotions() else 0
        val rating = player.getOrCreateGAW(config.gaw)
        val level = (rating.a + rating.b + rating.c + rating.d + rating.e) / 5
        @Suppress("SpellCheckingInspection")
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <galaxyatwargetratings>
                <ratings>
                    <rating>${rating.a}</rating>
                    <rating>${rating.b}</rating>
                    <rating>${rating.c}</rating>
                    <rating>${rating.d}</rating>
                    <rating>${rating.e}</rating>
                </ratings>
                <level>$level</level>
                <assets>
                    <assets>$promotions</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                </assets>
            </galaxyatwargetratings>
        """.trimIndent()
        val contentBuffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, contentBuffer)
        val headers = response.headers()
        headers.add("Content-Type", "text/xml;charset=UTF-8")
        headers.add("Content-Length", contentBuffer.readableBytes())
        ctx.writeAndFlush(response)
    }

    private fun handleGalaxyAtWarIncreaseRatings(ctx: ChannelHandlerContext, request: Request) {
        if (request.path.size < 3) {
            ctx.badRequest()
            return
        }
        val playerId = request.path[2].toIntOrNull()
        if (playerId == null) {
            ctx.badRequest()
            return
        }
        val player = transaction { Player.findById(playerId) }
        if (player == null) {
            ctx.badRequest()
            return
        }
        val promotions = if (config.gaw.enablePromotions) player.getTotalPromotions() else 0
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

        val level = (rating.a + rating.b + rating.c + rating.d + rating.e) / 5
        @Suppress("SpellCheckingInspection")
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <galaxyatwargetratings>
                <ratings>
                    <rating>${rating.a}</rating>
                    <rating>${rating.b}</rating>
                    <rating>${rating.c}</rating>
                    <rating>${rating.d}</rating>
                    <rating>${rating.e}</rating>
                </ratings>
                <level>$level</level>
                <assets>
                    <assets>$promotions</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                    <assets>0</assets>
                </assets>
            </galaxyatwargetratings>
        """.trimIndent()
        val contentBuffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, contentBuffer)
        val headers = response.headers()
        headers.add("Content-Type", "text/xml;charset=UTF-8")
        headers.add("Content-Length", contentBuffer.readableBytes())
        ctx.writeAndFlush(response)
    }
}