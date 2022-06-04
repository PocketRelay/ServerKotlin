@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.servers.DiscardServer
import com.jacobtread.kme.servers.http.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.nio.NioEventLoopGroup

fun main() {
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    Logger.init(GlobalConfig.logging)
    Logger.info("Starting ME3 Server")

    startDatabase()
    startRedirector(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup)
    startHttpServer(bossGroup, workerGroup)

    DiscardServer.start(bossGroup, workerGroup)
}

