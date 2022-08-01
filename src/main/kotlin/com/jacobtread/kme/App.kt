@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import io.netty.channel.nio.NioEventLoopGroup
import java.util.*

fun main() {
    Environment // Make sure environment is initialized

    // TODO: Implement auto-updater

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    startRedirector(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup)
    startHttpServer(bossGroup, workerGroup)

    System.gc() // Cleanup after initialization
}

