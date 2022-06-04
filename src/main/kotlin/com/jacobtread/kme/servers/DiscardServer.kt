package com.jacobtread.kme.servers

import com.jacobtread.kme.GlobalConfig
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import java.io.IOException

@Sharable
object DiscardServer : ChannelInboundHandlerAdapter() {

    /**
     * start Starts the discard server this is used for
     * discarding telemetry and ticker data from the client
     *
     * @param bossGroup The netty boss event loop group
     * @param workerGroup The netty worker event loop group
     */
    fun start(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
        try {
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(this)
                .bind(GlobalConfig.ports.discard)
                .addListener { Logger.info("Started Discard Server on port ${GlobalConfig.ports.discard}") }
        } catch (e: IOException) {
            Logger.error("Exception when starting discard server", e)
        }
    }

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
