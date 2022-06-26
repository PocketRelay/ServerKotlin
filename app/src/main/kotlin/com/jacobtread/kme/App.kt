@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.servers.http.startHttpServer
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.nio.NioEventLoopGroup

fun main() {
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    val loggingConfig = Environment.Config.logging

    Logger.init(loggingConfig.level, loggingConfig.save, loggingConfig.packets)
    Logger.info("Starting ME3 Server")

    startDatabase()
    startRedirector(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup)
    startHttpServer(bossGroup, workerGroup)
    startDiscardServer(bossGroup, workerGroup)
}

