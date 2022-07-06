package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.logging.Logger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

object PacketEncoder : MessageToByteEncoder<Packet>() {
    override fun isSharable(): Boolean = true

    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        if (Logger.logPackets) {
            try {
                Logger.debug("SENT PACKET ===========\n" + packetToBuilder(msg) + "\n======================")
            } catch (e: Throwable) {
                logPacketException("Failed to decode sent packet contents for debugging: ", msg, e)
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