package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerGalaxyAtWar
import com.jacobtread.kme.database.PlayerSettingsBase
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds
import com.jacobtread.xml.Node
import com.jacobtread.xml.xml
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList
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

        private fun ChannelHandlerContext.respond(
            content: Any? = null,
            status: HttpResponseStatus = HttpResponseStatus.OK,
            contentType: String? = null,
            headers: HashMap<String, String> = HashMap(),
        ) {
            val response = if (content != null && content !is HttpResponseStatus) {
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

                        val value = content.toString(false)
                        Unpooled.copiedBuffer(value, Charsets.UTF_8)
                    }
                    else -> throw IllegalArgumentException("Dont know how to handle unknown content type: $content")
                }
                headers["Content-Length"] = contentBuffer.readableBytes().toString()
                if (type.isNotEmpty()) headers["Content-Type"] = type

                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, contentBuffer)
            } else {
                headers["Content-Length"] = "0"
                if (content is HttpResponseStatus) {
                    DefaultFullHttpResponse(HttpVersion.HTTP_1_1, content)
                } else {
                    DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
                }
            }
            val headersOut = response.headers()
            headersOut.add("Access-Control-Allow-Origin", "*")
            headers.forEach { (key, value) -> headersOut.add(key, value) }
            write(response)
            flush()
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

    data class Request(val pathFull: String, val path: List<String>, val query: Map<String, String>)

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val url = msg.uri()
        Logger.debug("HTTP Request: $url")
       if (url.startsWith("/panel")) {
            handlePanelResponse(ctx, url, msg)
        }
    }

    fun handlePanelResponse(ctx: ChannelHandlerContext, url: String, request: HttpRequest) {
        val path = url.substring(6)
        if (path.isEmpty()) {
            return handleFallbackPage(ctx)
        }
        if (path.startsWith("/assets/")) {
            val assetName = path.substring(8)
            val resource = Data.getResourceOrNull("panel/assets/$assetName") ?: return ctx.respond(HttpResponseStatus.NOT_FOUND)
            val contentType = if (assetName.endsWith(".js")) {
                "text/javascript"
            } else if (assetName.endsWith(".css")) {
                "text/css"
            } else {
                Files.probeContentType(Paths.get(assetName))
            }
            ctx.respond(resource, contentType = contentType)
        } else if (path.startsWith("/api/")) {
            handleApiRoutes(ctx, path.substring(5), request)
        } else {
            handleFallbackPage(ctx)
        }
    }

    fun handleFallbackPage(ctx: ChannelHandlerContext) {
        val page = Data.getResource("panel/index.html")
        ctx.respond(page, contentType = "text/html;charset=UTF-8")
    }

    @Serializable
    data class PlayerSerial(
        val id: Int,
        val email: String,
        val displayName: String,
        val settings: PlayerSettingsBase,
    )

    fun handlePlayersList(ctx: ChannelHandlerContext, query: Map<String, String>) {
        val limit = query["limit"]?.toIntOrNull() ?: 10
        val offset = query["offset"]?.toIntOrNull() ?: 0
        val playerList = transaction {
            val playerList = ArrayList<PlayerSerial>()
            Player.all()
                .limit(limit, offset.toLong())
                .forEach {
                    playerList.add(
                        PlayerSerial(
                            it.playerId,
                            it.email,
                            it.displayName,
                            it.getSettingsBase()
                        )
                    )
                }
            playerList
        }
        val json = Json.encodeToString(playerList)
        ctx.respond(json, contentType = "application/json")
    }

    fun handleGetPlayerSettings(ctx: ChannelHandlerContext, query: Map<String, String>) {
        val playerId = query["id"]?.toIntOrNull() ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val player = transaction { Player.getById(playerId.toLong()) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val json = Json.encodeToString(SettingsSerializable(player.createSettingsMap()))

        ctx.respond(json, contentType = "application/json")
    }

    @Serializable
    data class SettingsSerializable(
        val settings: Map<String, String>,
    )

    fun handleSetPlayerSettings(ctx: ChannelHandlerContext, query: Map<String, String>, request: HttpRequest) {
        val playerId = query["id"]?.toIntOrNull() ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val player = transaction { Player.getById(playerId.toLong()) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        if (request !is FullHttpRequest) return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val contentBuffer = request.content()
        val bytes = ByteArray(contentBuffer.readableBytes())
        contentBuffer.readBytes(bytes)
        val settings: SettingsSerializable = Json.decodeFromString(SettingsSerializable.serializer(), bytes.decodeToString())
        settings.settings.forEach { (key, value) ->
            player.setSetting(key, value)
        }
    }

    fun handleUpdatePlayer(ctx: ChannelHandlerContext, request: HttpRequest) {
        if (request !is FullHttpRequest) return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        val contentBuffer = request.content()
        val bytes = ByteArray(contentBuffer.readableBytes())
        contentBuffer.readBytes(bytes)
        try {
            val player = Json.decodeFromString(PlayerSerial.serializer(), bytes.decodeToString())
            val existing = transaction { Player.findById(player.id) } ?: return ctx.respond(HttpResponseStatus.BAD_REQUEST)
            transaction {
                existing.displayName = player.displayName
                existing.email = player.email
                if (existing.settingsBase != null) {
                    val existingBase = existing.getSettingsBase()
                    val clone = PlayerSettingsBase(
                        player.settings.credits,
                        existingBase.c,
                        existingBase.d,
                        player.settings.creditsSpent,
                        existingBase.e,
                        player.settings.gamesPlayed,
                        player.settings.secondsPlayed,
                        existingBase.f,
                        player.settings.inventory
                    )
                    existing.settingsBase = clone.mapValue()
                }
            }
            ctx.respond(HttpResponseStatus.OK)
        } catch (e: SerializationException) {
            return ctx.respond(HttpResponseStatus.BAD_REQUEST)
        }
    }

    fun handleApiRoutes(ctx: ChannelHandlerContext, path: String, request: HttpRequest) {
        val urlParts = path.split('?', limit = 2)
        val parts = urlParts[0].split('/')
        if (parts.isEmpty()) return ctx.respond(HttpResponseStatus.NOT_FOUND)
        val query = if (urlParts.size > 1) parseQuery(urlParts[1]) else emptyMap()
        when (request.method()) {
            HttpMethod.GET -> {
                when (parts[0]) {
                    "players" -> return handlePlayersList(ctx, query)
                    "playerSettings" -> return handleGetPlayerSettings(ctx, query)
                }
            }
            HttpMethod.POST -> {
                when (parts[0]) {
                    "updatePlayer" -> return handleUpdatePlayer(ctx, request)
                    "setPlayerSettings" -> return handleSetPlayerSettings(ctx, query, request)
                }
            }
            HttpMethod.DELETE -> {

            }
            HttpMethod.PATCH -> {

            }
        }
    }

    fun handlePublicResponse(ctx: ChannelHandlerContext, url: String) {
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
                headers = hashMapOf(
                    "Accept-Ranges" to "bytes",
                    "ETag" to "524416-1333666807000"
                )
            )
        }
    }

    //region GAW Handlers

    //endregion
}