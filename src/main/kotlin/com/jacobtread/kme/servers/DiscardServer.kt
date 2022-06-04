package com.jacobtread.kme.servers

import com.jacobtread.kme.GlobalConfig
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException


/**
 * startDiscardServer Starts the discard server this is used
 * for discarding telemetry and ticker data from the client
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startDiscardServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val handler = object : ChannelInboundHandlerAdapter() {
            /**
             * isSharable Always true to make sure that this is allowed
             * to be shared amongst multiple channels
             *
             * @return Always true
             */
            override fun isSharable(): Boolean = true

            /**
             * channelRead Ignores anything sent to the discard server but
             * overrides this function to prevent it from being passed down
             * the pipeline
             *
             * @param ctx The channel handler context for this channel
             * @param msg The received message
             */
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {}
        }
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(handler)
            .bind(GlobalConfig.ports.discard)
            .addListener { Logger.info("Started Discard Server on port ${GlobalConfig.ports.discard}") }
    } catch (e: IOException) {
        Logger.error("Exception when starting discard server", e)
    }
}

