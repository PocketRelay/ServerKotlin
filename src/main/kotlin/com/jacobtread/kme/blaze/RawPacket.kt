package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled


class RawPacket(
    val rawComponent: Int,
    val rawCommand: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val rawContent: ByteArray,
) {
    val component = PacketComponent.from(rawComponent)
    val command = PacketCommand.from(rawCommand)
    val content: List<Tdf> by lazy {
        val buffer = Unpooled.wrappedBuffer(rawContent)
        val values = ArrayList<Tdf>()
        while (buffer.readableBytes() > 0) {
            values.add(Tdf.read(buffer))
        }
        values
    }

    override fun toString(): String {
        return "Packet (Component: $component ($rawComponent}), Command: $command ($rawCommand), Error; $error, QType: $qtype, Id: $id, Content: ${rawContent.contentToString()})"
    }

}