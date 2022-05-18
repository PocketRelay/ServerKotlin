package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import java.text.SimpleDateFormat
import java.util.*

class PacketDecoder : ByteToMessageDecoder() {
    private val printDateFormat = SimpleDateFormat("HH:mm:ss")

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        try {
            while (input.readableBytes() > 0) {
                val packet = Packet.read(input)

                val time = printDateFormat.format(Date())
                println("== $time == IN ======")
                println(PacketDumper.dump(packet))
                println("=".repeat(16 + time.length))

                out.add(packet)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}