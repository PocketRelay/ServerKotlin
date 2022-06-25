package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException

/**
 * startMITMServer Starts the MITM server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMITMServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(MITMHandler(workerGroup))
            // Bind the server to the host and port
            .bind(Environment.Config.ports.main)
            // Wait for the channel to bind
            .addListener { info("Started MITM Server on port ${Environment.Config.ports.main}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

class MITMHandler(val eventLoopGroup: NioEventLoopGroup) : SimpleChannelInboundHandler<Packet>() {

    var clientChannel: Channel? = null
    var officialChanel: Channel? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        channel.pipeline()
            // Add handler for decoding packets
            .addFirst(PacketDecoder())
            // Add handler for encoding packets
            .addLast(PacketEncoder())
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        clientChannel = ctx.channel()
        officialChanel = createOfficialConnection()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        clientChannel = null
        officialChanel?.apply {
            if (isOpen) close()
            officialChanel = null
        }
    }

    fun createOfficialConnection(): Channel {
        val config = Environment.Config.mitm
        val channelFuture = Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : SimpleChannelInboundHandler<Packet>() {
                override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
                    channelReadOffical(ctx, msg)
                }
            })
            .connect(config.host, config.port)
            .addListener {
                info("Created new MITM connection")
            }.sync()
        val channel = channelFuture.channel()
        val pipeline = channel.pipeline()
        pipeline.addFirst(PacketDecoder())
        if (config.secure) {
            val context = SslContextBuilder.forClient()
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1",  "TLSv1.1", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            pipeline.addFirst(context.newHandler(channel.alloc()))
        }
        pipeline.addLast(PacketEncoder())
        return channel
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        try {
            Logger.debug("RECEIVED PACKET FROM CLIENT =======\n" + packetToBuilder(msg) + "\n======================")
        } catch (e: Throwable) {
            logPacketException("Failed to decode incoming packet contents for debugging:", msg, e)
        }
        officialChanel?.apply {
            write(msg)
            flush()
        }
    }

    fun channelReadOffical(ctx: ChannelHandlerContext, msg: Packet) {
        try {
            Logger.debug("RECIEVED PACKET FROM EA =======\n" + packetToBuilder(msg) + "\n======================")
        } catch (e: Throwable) {
            logPacketException("Failed to decode incoming packet contents for debugging:", msg, e)
        }
        clientChannel?.apply {
            write(msg)
            flush()
        }
    }
}
