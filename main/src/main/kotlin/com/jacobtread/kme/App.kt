@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.nio.NioEventLoopGroup

// The version of KME
const val KME_VERSION = "1.0.0"

fun main() {
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    val config = loadConfigFile()
    Logger.init(config.logging)
    Logger.info("Starting ME3 Server")

    startDatabase(config.database)

    // Telemetry & Ticker servers just discard any contents they receive
    startHttpServer(bossGroup, workerGroup, config)
    startRedirector(bossGroup, workerGroup, config)
    startMainServer(bossGroup, workerGroup, config)
    startDiscardServer(bossGroup, workerGroup, intArrayOf(config.telemetry, config.ticker))
}
