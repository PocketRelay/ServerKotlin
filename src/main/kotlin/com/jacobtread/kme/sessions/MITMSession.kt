package com.jacobtread.kme.sessions

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.data.retriever.Retriever
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class MITMSession(
    private val clientChannel: Channel,
) : ChannelInboundHandlerAdapter() {

    private val serverChannel: Channel = createServerChannel()

    private fun createServerChannel(): Channel {
        val channel = Retriever.createOfficialChannel(object : ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg !is Packet) {
                    super.channelRead(ctx, msg)
                    return
                }
                channelReadOfficial(msg)
            }
        })
        if (channel != null) {
            PacketLogger.setEnabled(channel, true)
            return channel
        } else {
            Logger.fatal("Failed to create official server connection for MITM server")
        }
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
        // Optionally modify the contents of the packet or create custom response
        clientChannel.writeAndFlush(packet)
            .addListener { Packet.release(packet) }
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

        // Release the message when it's been written and flushed
        serverChannel.writeAndFlush(msg)
            .addListener { Packet.release(msg) }
    }
}
