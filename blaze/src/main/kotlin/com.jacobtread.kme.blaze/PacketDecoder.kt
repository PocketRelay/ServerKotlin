package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class PacketDecoder : ByteToMessageDecoder() {


    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        try {
            while (input.readableBytes() > 0) {
                val packet = RawPacket.read(input)

                println("RECEIVED PACKET:")
                println(PacketDumper.dump(packet))
                println()

                out.add(packet)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}