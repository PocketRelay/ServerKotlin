package com.jacobtread.kme.servers

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.retriever.Retriever
import com.jacobtread.kme.sessions.MITMSession
import com.jacobtread.kme.sessions.Session
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    val mitm = Environment.mitmEnabled

    // Ensure retriever is initialized for MITM mode.
    if (mitm) {
        Retriever
        PacketLogger.isEnabled = false
    }

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
                // Enable packet logging on this channel
                PacketLogger.setEnabled(ch, true)
                if (mitm) {
                    PacketLogger.setContext(ch, "MITM Connection to client")
                    ch.addPacketHandlers()
                        .addLast(MITMSession(ch))
                } else {
                    val session = Session(ch) // New session
                    val remoteAddress = ch.remoteAddress() // The remote address of the user
                    Logger.info("Main started new client session with $remoteAddress given id ${session.sessionId}")
                    // Add the packet handlers
                    ch.addPacketHandlers()
                        // Add handler for processing packets
                        .addLast(session)
                }
            }
        })
        // Bind the server to the host and port
        .bind(Environment.mainPort)
        // Wait for the channel to bind
        .addListener {
            val name = if (mitm) "MITM" else "Main"
            if (it.isSuccess) {
                Logger.info("Started $name server on port ${Environment.mainPort}")
            } else {
                val cause = it.cause()
                val reason = if (cause != null) (cause.message ?: cause.javaClass.simpleName) else "Unknown Reason"
                Logger.fatal("Unable to start $name server: $reason")
            }
        }
}