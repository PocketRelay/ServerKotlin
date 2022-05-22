package com.jacobtread.kme.servers.http

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.controllers.GAWController
import com.jacobtread.kme.servers.http.router.Router
import com.jacobtread.kme.servers.http.router.router
import com.jacobtread.kme.servers.http.router.routerGroup
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import java.io.IOException

fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val port = config.ports.http
        val router = router(config) {
            group(GAWController)
        }
        val initializer = HttpInitializer(router)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(initializer)
            .bind(port)
            .addListener { Logger.info("Started HTTP Server on port $port") }
    } catch (e: IOException) {
        Logger.error("Exception in HTTP server", e)
    }
}

@Sharable
class HttpInitializer(private val router: Router) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline()
            .addLast(HttpRequestDecoder())
            .addLast(HttpResponseEncoder())
            .addLast(router)
    }
}
