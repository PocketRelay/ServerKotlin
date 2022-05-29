package com.jacobtread.kme.servers.http

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.controllers.APIController
import com.jacobtread.kme.servers.http.controllers.GAWController
import com.jacobtread.kme.servers.http.router.groupedRoute
import com.jacobtread.kme.servers.http.router.router
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import java.io.IOException

/**
 * startHttpServer Starts the HTTP server
 *
 * @param bossGroup The boss event loop group to use
 * @param workerGroup The worker event loop group to use
 * @param config The server configuration
 */
fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val handler = HttpHandler(config)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(handler)
            .bind(config.ports.http)
            .addListener(handler)
    } catch (e: IOException) {
        Logger.error("Exception in HTTP server", e)
    }
}

/**
 * HttpInitializer Initializes the channel to accept http adds the decoder
 * and encoder and aggregator and the router to the channel
 *
 * @property router
 * @constructor Create empty HttpInitializer
 */
@Sharable
class HttpHandler(private val config: Config) : ChannelInitializer<Channel>(), FutureListener<Void> {

    /**
     * router Initializing the router paths
     */
    val router = router(config) {
        +GAWController // Galaxy at war routing group
        if (config.panel.enabled) { // If the panel is enabled
            +groupedRoute("panel") { // Panel routing group
                +APIController // API routing group
                get(":*") { _, request -> // Catchall for static assets falling back on index.html
                    val path = request.param("*")
                    request.static(path, "panel", "index.html", "panel")
                }
            }
        }
        // Contents catchall for the assets ME3 fetches
        get("content/:*") { _, request ->
            val path = request.param("*")
            val fileName = path.substringAfterLast('/')
            request.setHeader("Accept-Ranges", "bytes")
            request.setHeader("ETag", "524416-1333666807000")
            request.static(fileName, "public")
        }
    }

    /**
     * initChannel Initializes the channel adding the http encoders,
     * decoders and content aggregator and the router
     *
     * @param ch
     */
    override fun initChannel(ch: Channel) {
        ch.pipeline()
            .addLast(HttpRequestDecoder())
            .addLast(HttpResponseEncoder())
            .addLast(HttpObjectAggregator(1024 * 8))
            .addLast(router)
    }

    /**
     * operationComplete For listening to the future of the
     * http server bind completion
     *
     * @param future Ignored
     */
    override fun operationComplete(future: Future<Void>) {
        Logger.info("Started HTTP Server on port ${config.ports.http}")
    }
}
