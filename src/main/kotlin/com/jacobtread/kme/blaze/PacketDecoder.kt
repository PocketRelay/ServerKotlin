package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class PacketDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        println("Readable: ${input.readableBytes()}")
        println("Byte Order: ${input.order()}")
        while (input.readableBytes() > 0) {

            val packet = readRawPacket(input)
            out.add(packet)
        }
    }
}