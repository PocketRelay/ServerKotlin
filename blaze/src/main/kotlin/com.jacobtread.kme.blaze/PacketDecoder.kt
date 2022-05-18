package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import java.text.SimpleDateFormat
import java.util.*

class PacketDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        try {
            while (input.readableBytes() > 0) {
                out.add(Packet.read(input))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}