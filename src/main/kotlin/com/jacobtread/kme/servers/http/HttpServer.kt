package com.jacobtread.kme.servers.http

import com.jacobtread.kme.GlobalConfig
import com.jacobtread.kme.servers.http.router.createRouter
import com.jacobtread.kme.servers.http.routes.*
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
fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val handler = HttpHandler()
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(handler)
            .bind(GlobalConfig.ports.http)
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
class HttpHandler : ChannelInitializer<Channel>(), FutureListener<Void> {

    /**
     * router Initializing the router paths
     */
    val router = createRouter {
        routeGroupGAW() // Galaxy at war routing group
        if (GlobalConfig.panel.enabled) { // If the panel is enabled
            routeGroupPanel() // Panel routing group
        }
        routeContents() // Contents routing
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
        Logger.info("Started HTTP Server on port ${GlobalConfig.ports.http}")
    }
}
