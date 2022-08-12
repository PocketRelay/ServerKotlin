@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.command.CommandManager
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import com.jacobtread.kme.utils.logging.Logger
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

    CommandManager.start()
}

