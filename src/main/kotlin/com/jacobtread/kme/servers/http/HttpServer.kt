package com.jacobtread.kme.servers.http

import com.jacobtread.kme.Environment
import com.jacobtread.kme.servers.http.router.createRouter
import com.jacobtread.kme.servers.http.routes.routeContents
import com.jacobtread.kme.servers.http.routes.routeGroupGAW
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

/**
 * startHttpServer Starts the HTTP server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startHttpServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val router = createRouter {
            routeGroupGAW() // Galaxy at war routing group
            routeContents() // Contents routing
        }
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(router)
            .bind(Environment.httpPort)
            .addListener { Logger.info("Started HTTP Server on port ${Environment.httpPort}") }
    } catch (e: IOException) {
        Logger.error("Exception when starting HTTP server", e)
    }
}