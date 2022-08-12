package com.jacobtread.kme.sessions

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.kme.data.retriever.Retriever
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey

/**
 * MITM (Man-In-The-Middle) session handler. Takes in a clientChannel parameter.
 * This is the channel of the connecting client whose traffic needs to be sent
 * to the official servers.
 *
 * When this is initialized a connection to the official server is created and
 * any packets sent by the clientChannel will be sent to the official server.
 *
 * @constructor Creates a new MITM session
 *
 * @param clientChannel The connected client channel
 */
class MITMSession(clientChannel: Channel) : ChannelInboundHandlerAdapter() {

    companion object {
        /**
         * This attribute is used to determine which channel that packets
         * recieved on each channel should be sent to.
         */
        private val TARGET_CHANNEL = AttributeKey.newInstance<Channel>("TC")

        /**
         * This attribute is used to determine the identifiying message for
         * logging that a packet was decoded from the specific channel.
         */
        private val RECEIVED_MSG = AttributeKey.newInstance<String>("TCM")
    }

    init {
        // Create an official server connection
        val serverChannel: Channel = Retriever.createOfficialChannel(this)
            ?: Logger.fatal("Failed to create official server connection for MITM server")

        // Set up the client and server channels
        serverChannel.apply {
            PacketLogger.setEnabled(this, true)
            PacketLogger.setContext(this, "Connection to EA")
            attr(TARGET_CHANNEL).set(clientChannel)
            attr(RECEIVED_MSG).set("DECODED FROM CLIENT")
        }

        clientChannel.apply {
            PacketLogger.setContext(this, "Connection to Client")
            attr(TARGET_CHANNEL).set(serverChannel)
            attr(RECEIVED_MSG).set("DECODED FROM EA")
        }
    }

    /**
     * Handles recieved messages. If the message is a packet then it
     * is written to the target channel for that channel then it
     * is flushed and the packet is released
     *
     * @param ctx The channel handler context
     * @param msg The recieved message
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is Packet) {
            val channel = ctx.channel()

            val recievedMessage = channel.attr(RECEIVED_MSG).get() ?: "DECODED"
            PacketLogger.log(recievedMessage, channel, msg)

            val targetChannel = channel.attr(TARGET_CHANNEL).get() ?: return
            targetChannel.writeAndFlush(msg)
                .addListener { Packet.release(msg) }
        } else {
            ctx.fireChannelRead(msg)
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
        val channel = ctx.channel()
        val targetChannel = channel.attr(TARGET_CHANNEL)
            .get() ?: return
        if (targetChannel.isOpen) {
            targetChannel.close()
        }
    }
}
