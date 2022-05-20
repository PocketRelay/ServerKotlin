package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.logging.Logger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class PacketDecoder : ByteToMessageDecoder() {


    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        try {
            while (input.readableBytes() > 0) {
                val cursorStart = input.readerIndex()

                val length = input.readUnsignedShort();
                if (input.readableBytes() < (length + 10)) {
                    input.readerIndex(cursorStart)
                    return
                }

                val packet = Packet.readHead(length, input)
                val contentLength = packet.rawContent.size

                if (input.readableBytes() >= contentLength) {
                    input.readBytes(packet.rawContent)
                    if (Logger.isLogPackets) {
                        Logger.debug("RECIEVED PACKET =======\n" + packetToBuilder(packet) + "\n======================")
                    }
                    out.add(packet)
                } else {
                    input.readerIndex(cursorStart)
                    return
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}