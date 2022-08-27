package com.jacobtread.relay.servers

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.relay.Environment
import com.jacobtread.relay.utils.logging.Logger
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * startRedirector
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startQOSServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup): CompletableFuture<Void> {
    val startupFuture = CompletableFuture<Void>()
    try {
        val listenPort = 17499
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    val remoteAddress = ch.remoteAddress()
                    Logger.debug("Connection at $remoteAddress to QOS Server")
                    PacketLogger.setEnabled(ch, true)
                    ch.pipeline().addLast(QOSHandler(ch))
                }
            })
            .bind(listenPort)
            .addListener {
                Logger.info("Started QOS on port 17499")
                startupFuture.complete(null)
            }
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start telemetry server: $reason")
    }
    return startupFuture
}

class QOSHandler(val clientChannel: Channel) : ChannelInboundHandlerAdapter() {

    private fun createServerChannel(): Channel {
        val channelFuture = Bootstrap()
            .group(NioEventLoopGroup())
            .channel(NioSocketChannel::class.java)
            .handler(OfficialHandler())
            .connect("162.244.53.174", 17499)
            .sync()
        return channelFuture.channel()
    }
    private var serverChannel: Channel = createServerChannel()

    inner class OfficialHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is ByteBuf) {
                Logger.info("[QOSSERVER] [SERVER] Sent bytes ${msg.readableBytes()}")
            }
            clientChannel.writeAndFlush(msg)
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            Logger.info("[QOSSERVER] [CLIENT] Sent bytes ${msg.readableBytes()}")
        }
        if (serverChannel.isOpen) {
            serverChannel.writeAndFlush(msg)
        } else {
            serverChannel = createServerChannel()
        }
    }
}