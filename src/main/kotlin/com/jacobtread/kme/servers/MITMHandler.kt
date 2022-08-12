package com.jacobtread.kme.servers

import com.jacobtread.blaze.clientPacket
import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.blaze.respond
import com.jacobtread.blaze.text
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.data.retriever.Retriever
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel


class MITMHandler(
    private val clientChannel: Channel,
) : ChannelInboundHandlerAdapter() {

    private val serverChannel: Channel = createServerChannel()
    private var forwardHttp: Boolean = false
    private var unlockCheat: Boolean = false

    private fun createServerChannel(): Channel {
        return Retriever.createOfficialChannel(object : ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg !is Packet) {
                    super.channelRead(ctx, msg)
                    return
                }
                channelReadOfficial(msg)
            }
        }) ?: Logger.fatal("Failed to create official server connection for MITM server")
    }

    /**
     * Handles inactivity on the channel which closes the
     * server channel connection
     *
     * @param ctx
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        if (serverChannel.isOpen) {
            serverChannel.close()
        }
    }

    /**
     * Handles reading the packets from the official server channel and writing
     * them to the client channel. Also handles HTTP forwarding
     *
     * @param packet The recieved packet
     */
    private fun channelReadOfficial(packet: Packet) {
        PacketLogger.log("DECODED FROM EA SERVER", serverChannel, packet)

        if (tryForwardHttp(packet)) return

        // Optionally modify the contents of the packet or create custom response
        clientChannel.writeAndFlush(packet)
            .addListener { Packet.release(packet) }
    }

    /**
     * Tries to activate the "unlock everything cheat". Triggered when
     * the client asks the server to load all settings. Sends lots of
     * packets which updates all the client settings and data for
     * inventory, classes, challenges, etc.
     *
     * @param packet The packet to check
     */
    private fun tryUnlockCheat(packet: Packet) {
        if (!unlockCheat) return
        if (packet.component == Components.UTIL && packet.command == Commands.USER_SETTINGS_LOAD_ALL) {

            Logger.info("Unlocking everything with cheat.")

            var id = 999

            // Base settings cheat (Unlocks all inventory items and gives max currency value)
            serverChannel.writeAndFlush(
                clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    val baseBuilder = StringBuilder("20;4;")
                        .append(Int.MAX_VALUE - (Int.MAX_VALUE / 24))
                        .append(";-1;0;0;0;540;4320000;0;")
                    repeat(671) {
                        baseBuilder.append("FF")
                    }
                    text("DATA", baseBuilder.toString())
                    text("KEY", "Base")
                    number("UID", 0)
                }
            )


            // Class level and promotions chea
            val classNames = arrayOf("Adept", "Soldier", "Engineer", "Sentinel", "Infiltrator", "Vanguard")
            classNames.forEachIndexed { index, className ->
                serverChannel.write(
                    clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                        val builder = StringBuilder("20;4;")
                            .append(className)
                            .append(";20;0;")
                            .append("900")
                        text("DATA", builder.toString())
                        text("KEY", "class${index + 1}")
                        number("UID", 0)
                    }
                )
            }
            serverChannel.flush()

            // Challenge completion cheat
            serverChannel.writeAndFlush(
                clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    val builder = StringBuilder("22")
                    repeat(221) { builder.append(",255") }
                    text("DATA", builder.toString())
                    text("KEY", "cscompletion")
                    number("UID", 0)
                }
            )

            // Challenge completion cheat
            serverChannel.writeAndFlush(
                clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    val builder = StringBuilder("22")
                    repeat(746) { builder.append(",255") }
                    text("DATA", builder.toString())
                    text("KEY", "Completion")
                    number("UID", 0)
                }
            )
        }
    }

    /**
     * Tries to forward the HTTP traffic that would normally go to the
     * official http servers to the localhost servers. This returns
     * true if the redirect was applied and false if not. This value
     * depends on the [forwardHttp] field
     *
     * @param packet The receieved packet
     * @return Whether to ignore sending this packet to the client
     */
    private fun tryForwardHttp(packet: Packet): Boolean {
        if (forwardHttp
            && packet.component == Components.UTIL
            && packet.command == Commands.FETCH_CLIENT_CONFIG
        ) {
            val type = packet.text("CFID")
            if (type == "ME3_DATA") {
                clientChannel.writeAndFlush(
                    packet.respond {
                        map("CONF", Data.createDataConfig())
                    }
                )
                return true
            }
        }
        return false
    }

    /**
     * Handles reading the packets from the clent and writing
     * them to the official server channel. Also handles the
     * unlocking cheat logic
     *
     * @param ctx The channel handler context
     * @param msg The receieved packet
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) {
            super.channelRead(ctx, msg)
            return
        }

        PacketLogger.log("DECODED FROM CLIENT", clientChannel, msg)

        tryUnlockCheat(msg)

        // Release the message when it's been written and flushed
        serverChannel.writeAndFlush(msg)
            .addListener { Packet.release(msg) }
    }
}
