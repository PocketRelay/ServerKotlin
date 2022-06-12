package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.logging.Logger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncoder : MessageToByteEncoder<Packet>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        if (Logger.isLogPackets) {
            try {
                Logger.debug("SENT PACKET ===========\n" + packetToBuilder(msg) + "\n======================")
            } catch (e: Throwable) {
                Logger.warn("Failed to decode sent packet contents for debugging: ")
                Logger.warn("Packet Information ==================================")
                Logger.warn("Component: ${msg.component.toString(16)} ${Components.getName(msg.component)}")
                Logger.warn("Command: ${msg.command.toString(16)} ${Commands.getName(msg.component, msg.command)}")
                Logger.warn("Error: ${msg.command.toString(16)}")
                val typeName = when (msg.type) {
                    Packet.INCOMING_TYPE -> "INCOMING"
                    Packet.ERROR_TYPE -> "ERROR"
                    Packet.UNIQUE_TYPE -> "UNIQUE"
                    Packet.RESPONSE_TYPE -> "RESPONSE"
                    else -> "UNKNOWN"
                }
                Logger.warn("Type: $typeName (${msg.type.toString(16)})")
                Logger.warn("ID: ${msg.id.toString(16)}")
                Logger.warn("Cause: ${e.message}")
                Logger.warn(e.stackTraceToString())
                Logger.warn("=====================================================")
            }
        }
        val content = msg.contentBuffer
        val length = content.readableBytes()
        out.writeByte((length and 0xFFFF) shr 8)
        out.writeByte((length and 0xFF))
        out.writeShort(msg.component)
        out.writeShort(msg.command)
        out.writeShort(msg.error)
        out.writeByte(msg.type shr 8)
        out.writeByte(if (length > 0xFFFF) 0x10 else 0x00)
        out.writeShort(msg.id)
        if (length > 0xFFFF) {
            out.writeByte(((length.toLong() and 0xFF000000) shr 24).toInt())
            out.writeByte((length and 0x00FF0000) shr 16)
        }
        out.writeBytes(content)
        content.release()
        ctx.flush()
    }
}