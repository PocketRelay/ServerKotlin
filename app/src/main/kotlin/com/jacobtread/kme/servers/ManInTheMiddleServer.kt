package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.data.Data
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
                    ch.pipeline().addLast(MITMHandler(workerGroup))
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

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
        val channel = ctx.channel()
        channel.pipeline()
            .addFirst(PacketDecoder())
            .addLast(PacketEncoder)
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

    private fun createOfficialConnection(): Channel {
        val channelFuture = Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {
                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    channelReadOffical(msg)
                }
            })
            .connect(Environment.mitmHost, Environment.mitmPort)
            .sync()
        info("Created new MITM connection")
        val channel = channelFuture.channel()
        val pipeline = channel.pipeline();
        pipeline.addFirst(PacketDecoder())
        if (Environment.mitmSecure) {
            val context = SslContextBuilder.forClient()
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            pipeline.addFirst(context.newHandler(channel.alloc()))
        }
        pipeline.addLast(PacketEncoder)
        return channel
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) return
        try {
            Logger.debug("RECEIVED PACKET FROM CLIENT =======\n" + packetToBuilder(msg) + "\n======================")
        } catch (e: Throwable) {
            logPacketException("Failed to decode incoming packet contents for debugging:", msg, e)
        }
        if (msg.component == Components.UTIL && msg.command == Commands.USER_SETTINGS_LOAD_ALL) {
            // Unlock everything cheat
//            createUnlockPackets()
        }
//          Forward HTTP traffic to local
//        if (msg.component == Components.UTIL && msg.command == Commands.FETCH_CLIENT_CONFIG) {
//            msg.contentBuffer.retain()
//            val type = msg.text("CFID")
//            if (type == "ME3_DATA") {
//                clientChannel?.apply {
//                    write(msg.respond  {
//                        map("CONF", Data.createDataConfig())
//                    })
//                    flush()
//                }
//                return
//            }
//
//        }

        officialChanel?.apply {
            write(msg)
            flush()
        }
    }

    private fun createUnlockPackets() {
        info("UNLOCKING EVERYTHING")
        var id = 999
        officialChanel?.apply {
            write(clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                val out = "20;4;${Int.MAX_VALUE};-1;0;0;0;50;180000;0;${"f".repeat(1342)}"
                text("DATA", out)
                text("KEY", "Base")
                number("UID", 0x0)
            })
            flush()
            val names = arrayOf("Adept", "Soldier", "Engineer", "Sentinel", "Infiltrator", "Vanguard")
            for (i in 1..6) {
                val name = names[i - 1]
                val out = "20;4;$name;20;0;${Integer.MAX_VALUE}"
                write(clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    text("DATA", out)
                    text("KEY", "class$i")
                    number("UID", 0x0)
                })
                flush()
            }
        }
    }


    fun channelReadOffical(msg: Any) {
        if (msg !is Packet) return
        try {
            Logger.logIfDebug { "RECIEVED PACKET FROM EA =======\n" + packetToBuilder(msg) + "\n======================" }
        } catch (e: Throwable) {
            logPacketException("Failed to decode incoming packet contents for debugging:", msg, e)
        }
        clientChannel?.apply {
            write(msg)
            flush()
        }
    }
}
