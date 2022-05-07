package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncoder : MessageToByteEncoder<RawPacket>() {
    override fun encode(ctx: ChannelHandlerContext, msg: RawPacket, out: ByteBuf) {
        val content = msg.rawContent
        val length = content.size
        out.writeByte((length and 0xFFFF) shr 8)
        out.writeByte((length and 0xFF))
        out.writeShort(msg.rawComponent)
        out.writeShort(msg.rawCommand)
        out.writeShort(msg.error)
        out.writeByte(msg.qtype shr 8)
        out.writeByte(if (length > 0xFFFF) 0x10 else 0x00 )
        out.writeShort(msg.id)
        if (length > 0xFFFF) {
            out.writeByte(((length.toLong() and 0xFF000000) shr 24).toInt())
            out.writeByte((length and 0x00FF0000) shr 16)
        }
        out.writeBytes(content)
        ctx.flush()
        println("WROTE PACKET")
    }
}