@file:JvmName("App")

package com.jacobtread.relay

import com.jacobtread.relay.http.startHttpServer
import com.jacobtread.relay.servers.startMainServer
import com.jacobtread.relay.servers.startRedirector
import com.jacobtread.relay.utils.logging.Logger
import io.netty.channel.nio.NioEventLoopGroup

fun main() {
    Environment // Make sure environment is initialized

    // TODO: Implement auto-updater

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    startRedirector(bossGroup, workerGroup)
    startHttpServer(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup)

    System.gc() // Cleanup after initialization

    Logger.info("Server startup complete.")
}

