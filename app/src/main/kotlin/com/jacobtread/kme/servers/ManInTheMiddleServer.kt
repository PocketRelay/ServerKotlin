package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.logPacketException
import com.jacobtread.kme.blaze.packetToBuilder
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
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
        if (Logger.logPackets && Logger.debugEnabled) {
            Logger.warn("WARNING: You have packet logging enabled while MITM is enabled.")
            Logger.warn("this will flood your logs with lots of repeated packets and")
            Logger.warn("I recommend you disable packet logging while using MITM")
        }
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline().addFirst(MITMHandler(workerGroup))
                }
            })
            // Bind the server to the host and port
            .bind(Environment.mainPort)
            // Wait for the channel to bind
            .addListener { info("Started MITM Server on port ${Environment.mainPort}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

private fun obtainAddressAutomatic(eventLoopGroup: NioEventLoopGroup) {
    info("Connecting to official redirector server for connection information")
    TODO("NOT YET IMPLEMENTED")
}

class MITMHandler(private val eventLoopGroup: NioEventLoopGroup) : ChannelInboundHandlerAdapter() {

    var clientChannel: Channel? = null
    var officialChanel: Channel? = null

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

    private fun createOfficialConnection(): Channel {
        val channelFuture = Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {
                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    if (msg is Packet) {
                        channelReadOffical(msg)
                    }
                }
            })
            .connect(Environment.mitmHost, Environment.mitmPort)
            .addListener {
                info("Created new MITM connection")
            }.sync()
        val channel = channelFuture.channel()
        if (Environment.mitmSecure) {
            val context = SslContextBuilder.forClient()
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            channel.pipeline().addFirst(context.newHandler(channel.alloc()))
        }
        return channel
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) return
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

    fun channelReadOffical(msg: Packet) {
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
