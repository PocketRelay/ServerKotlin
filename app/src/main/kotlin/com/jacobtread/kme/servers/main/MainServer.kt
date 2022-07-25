package com.jacobtread.kme.servers.main

import com.jacobtread.blaze.PacketDecoder
import com.jacobtread.blaze.PacketEncoder
import com.jacobtread.kme.Environment
import com.jacobtread.kme.servers.startMITMServer
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
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
            .childHandler(MainInitializer())
            // Bind the server to the host and port
            .bind(Environment.mainPort)
            // Wait for the channel to bind
            .addListener { info("Started Main Server on port ${Environment.mainPort}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

/**
 * MainInitializer Channel Initializer for main server clients.
 * Creates sessions for the user as well as adding packet handlers
 * and the MainClient handler
 *
 * @constructor Create empty MainClientInitializer
 */
@Sharable
class MainInitializer : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val remoteAddress = ch.remoteAddress() // The remote address of the user
        val session = Session(ch)
        info("Main started new client session with $remoteAddress given id ${session.sessionId}")
        ch.pipeline()
            // Add handler for decoding packet
            .addLast(PacketDecoder())
            // Add handler for processing packets
            .addLast(session)
            .addLast(PacketEncoder)
    }
}