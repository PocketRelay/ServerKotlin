package com.jacobtread.kme.servers

import com.jacobtread.kme.utils.customThreadFactory
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import java.io.IOException


fun startHttpServer() {
    val bossGroup = NioEventLoopGroup(customThreadFactory("HTTP Boss #{ID}"))
    val workerGroup = NioEventLoopGroup(customThreadFactory("HTTP Worker #{ID}"))
    val bootstrap = ServerBootstrap() // Create a server bootstrap
    try {
        val bind = bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline()
                        // Add handler for processing ticker data
                        .addLast(HttpRequestDecoder())
                        .addLast(HttpResponseEncoder())
                        .addLast(HTTPHandler())
                }
            })
            // Bind the server to the host and port
            .bind(80)
            // Wait for the channel to bind
            .sync();
        Logger.info("Started HTTP Server on port 80")
        bind.channel()
            // Get the closing future
            .closeFuture()
            // Wait for the closing
            .sync()
    } catch (e: IOException) {
        Logger.error("Exception in HTTP server", e)
    }
}

private class HTTPHandler : SimpleChannelInboundHandler<HttpRequest>() {

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val method = msg.method()
        if (method != HttpMethod.GET) {
            respond404(ctx)
            return
        }
        val url = msg.uri()
        if (url.startsWith("/wal/masseffect-gaw-pc")) {
            gawResponse(ctx, url)
        } else {
            fileSystemResponse(ctx, url)
        }
    }

    fun fileSystemResponse(ctx: ChannelHandlerContext, url: String) {
        val fileName = url.substringAfterLast('/')
        val pathName = "/public/$fileName"
        val inputStream = HTTPHandler::class.java.getResourceAsStream(pathName);
        if (inputStream == null) {
            respond404(ctx)
        } else {
            val contents = inputStream.readAllBytes()
            val content = Unpooled.wrappedBuffer(contents)
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content)
            val headers = response.headers()
            headers.add("Accept-Ranges", "bytes")
            headers.add("ETag", "524416-1333666807000")
            ctx.writeAndFlush(response)
        }
    }

    fun respond404(ctx: ChannelHandlerContext) {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
        ctx.write(response)
    }

    fun gawResponse(ctx: ChannelHandlerContext, url: String) {
        val path = url.substring(23)
        val parts = path.split('/')
        if (parts.size < 2) {
            respond404(ctx)
            return
        }
        println("GAW REQUEST $path ${parts.joinToString(", ") { it }}")
    }
}