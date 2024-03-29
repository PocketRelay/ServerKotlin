package com.jacobtread.relay.servers

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.relay.Environment
import com.jacobtread.relay.data.retriever.Retriever
import com.jacobtread.relay.sessions.MITMSession
import com.jacobtread.relay.sessions.Session
import com.jacobtread.relay.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException
import java.util.concurrent.CompletableFuture as Future

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup): Future<Void>{
    val startupFuture = Future<Void>()
    val mitm = Environment.mitmEnabled

    val name: String
    if (mitm) {
        Retriever // Ensure retriever is initialized for MITM mode.
        PacketLogger.isEnabled = false
        name = "MITM"
    } else {
        name = "Main"
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
            .addListener {
                Logger.info("Started $name server on port ${Environment.mainPort}")
                startupFuture.complete(null)
            }
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start $name server: $reason")
    }
    return startupFuture
}