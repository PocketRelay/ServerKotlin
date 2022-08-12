package com.jacobtread.kme.servers

import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.kme.Environment
import com.jacobtread.kme.game.Session
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    if (Environment.mitmEnabled) { // If MITM is enabled
        startMITMServer(bossGroup, workerGroup)
        return // Don't create the normal main server
    }
    try {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun isSharable(): Boolean = true

                /**
                 * Initializes the channel that has connected. In this case
                 * the packet handlers and a [Session] handler are added to
                 * the channel and a connection message is logged.
                 *
                 * @param ch The channel to initialize
                 */
                override fun initChannel(ch: Channel) {
                    val session = Session(ch) // New session
                    val remoteAddress = ch.remoteAddress() // The remote address of the user
                    info("Main started new client session with $remoteAddress given id ${session.sessionId}")
                    // Add the packet handlers
                    ch.addPacketHandlers()
                        // Add handler for processing packets
                        .addLast(session)
                }
            })
            // Bind the server to the host and port
            .bind(Environment.mainPort)
            // Wait for the channel to bind
            .addListener { info("Started Main Server on port ${Environment.mainPort}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}