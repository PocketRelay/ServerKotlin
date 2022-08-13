package com.jacobtread.kme.servers

import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.group
import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.blaze.respond
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.createServerSslContext
import com.jacobtread.kme.utils.getIPv4Encoded
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.DecoderException
import java.io.IOException
import javax.net.ssl.SSLException

/**
 * startRedirector
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val listenPort = Environment.redirectorPort
        val externalAddress = Environment.externalAddress
        val targetAddress = getIPv4Encoded(externalAddress)
        val context = createServerSslContext()
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    val remoteAddress = ch.remoteAddress()
                    Logger.debug("Connection at $remoteAddress to Redirector Server")
                    PacketLogger.setEnabled(ch, true)
                    ch.addPacketHandlers(context)
                        .addLast(RedirectorHandler(ch, targetAddress))
                }
            })
            .bind(listenPort)
            .sync()
        Logger.info("Started Redirector on port ${Environment.redirectorPort}")
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start redirector server: $reason")
    }
}


/**
 * This handler checks incoming packets to see if they are
 * [Components.REDIRECTOR] / [Commands.GET_SERVER_INSTANCE]
 * and if they are it sends them the connection details of
 * the main server (i.e. The host address / ip and port)
 *
 * @constructor Creates a new redirector handler
 */
class RedirectorHandler(
    private val channel: Channel,
    private val targetAddress: ULong,
) : ChannelInboundHandlerAdapter() {
    private val isHostname: Boolean = targetAddress == 0uL

    /**
     * Handles interacting with the result of a read if the
     * read value is a packet then it is handled and if it's a GET_SERVER_INSTANCE
     * REDIRECTOR packet it is responded to with the redirect details
     *
     * @param ctx The channel context
     * @param msg The message that was read
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is Packet) {
            routePacket(channel, msg)
            ctx.flush()
            Packet.release(msg)
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    @PacketHandler(Components.REDIRECTOR, Commands.GET_SERVER_INSTANCE)
    fun handleGetServerInstance(packet: Packet) {
        channel.write(
            packet.respond {
                optional("ADDR", group("VALU") {
                    if (isHostname) {
                        text("HOST", Environment.externalAddress)
                    } else {
                        number("IP", targetAddress)
                    }
                    number("PORT", Environment.mainPort)
                })
                // Determines if SSLv3 should be used when connecting to the main server
                bool("SECU", false)
                bool("XDNS", false)
            }
        )
    }

    @Suppress("OVERRIDE_DEPRECATION") // Not actually depreciated.
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        if (cause == null) return
        val channel = ctx.channel()
        val ipAddress = channel.remoteAddress()
        if (cause is DecoderException) {
            val underlying = cause.cause
            if (underlying != null) {
                if (underlying is SSLException) {
                    Logger.debug("Connection at $ipAddress tried to connect without vaid SSL (Did someone try to connect with a browser?)")
                    ctx.close()
                    return
                }
            }
        } else if (cause is IOException) {
            val message = cause.message
            if (message != null && message.startsWith("Connection reset")) {
                Logger.debug("Connection to client at $ipAddress lost")
                return
            }
        }
        ctx.close()
    }
}
