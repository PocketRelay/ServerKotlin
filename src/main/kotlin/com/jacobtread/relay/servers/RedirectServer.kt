package com.jacobtread.relay.servers

import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.group
import com.jacobtread.blaze.handler.PacketNettyHandler
import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.blaze.respond
import com.jacobtread.relay.Environment
import com.jacobtread.relay.data.blaze.Commands
import com.jacobtread.relay.data.blaze.Components
import com.jacobtread.relay.utils.createServerSslContext
import com.jacobtread.relay.utils.getIPv4Encoded
import com.jacobtread.relay.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

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
@PacketProcessor
class RedirectorHandler(
    override val channel: Channel,
    private val targetAddress: ULong,
) : PacketNettyHandler() {
    private val isHostname: Boolean = targetAddress == 0uL

    @PacketHandler(Components.REDIRECTOR, Commands.GET_SERVER_INSTANCE)
    fun handleGetServerInstance(packet: Packet) {
        push(
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

    override fun handlePacket(ctx: ChannelHandlerContext, packet: Packet) {
        routePacket(channel, packet)
    }

    override fun handleConnectionLost(ctx: ChannelHandlerContext) {
        val ipAddress = channel.remoteAddress()
        Logger.debug("Connection to client at $ipAddress lost")
    }
}
