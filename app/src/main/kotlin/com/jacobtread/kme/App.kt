@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.servers.http.startHttpServer
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import io.netty.channel.nio.NioEventLoopGroup

fun main() {
    Environment // Make sure environment is initialized

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    startRedirector(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup)
    startHttpServer(bossGroup, workerGroup)
    startDiscardServer(bossGroup, workerGroup)
}

