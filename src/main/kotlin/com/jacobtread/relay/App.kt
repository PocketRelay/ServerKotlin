@file:JvmName("App")

package com.jacobtread.relay

import com.jacobtread.relay.http.startHttpServer
import com.jacobtread.relay.servers.startMainServer
import com.jacobtread.relay.servers.startQOSServer
import com.jacobtread.relay.servers.startRedirector
import com.jacobtread.relay.servers.startTelemetryServer
import com.jacobtread.relay.utils.logging.Logger
import io.netty.channel.nio.NioEventLoopGroup
import startSyncServer
import java.util.concurrent.CompletableFuture as Future

fun main() {
    Environment // Make sure environment is initialized

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    Future.allOf(
        startRedirector(bossGroup, workerGroup),
        startHttpServer(bossGroup, workerGroup),
        startMainServer(bossGroup, workerGroup),
        startSyncServer(bossGroup, workerGroup),
        startTelemetryServer(bossGroup, workerGroup),
        startQOSServer(bossGroup, workerGroup)
    ).get()

    System.gc() // Cleanup after initialization
    Logger.info("Server startup complete.")
}

