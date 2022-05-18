package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.Tdf
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class Packet(
    val rawComponent: Int,
    val rawCommand: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val rawContent: ByteArray,
) : TdfContainer {
    companion object {
        fun read(input: ByteBuf): Packet {
            val length = input.readUnsignedShort();
            val component = input.readUnsignedShort()
            val command = input.readUnsignedShort()
            val error = input.readUnsignedShort()
            val qtype = input.readUnsignedShort()
            val id = input.readUnsignedShort()
            val extLength = if ((qtype and 0x10) != 0) input.readUnsignedShort() else 0
            val contentLength = length + (extLength shl 16)
            val content = ByteArray(contentLength)
            input.readBytes(content)
            return Packet(component, command, error, qtype, id, content)
        }
    }


    val component = Component.from(rawComponent)
    val command = Command.from(rawComponent, rawCommand)
    val content: List<Tdf<*>> by lazy {
        val buffer = Unpooled.wrappedBuffer(rawContent)
        val values = ArrayList<Tdf<*>>()
        try {
            while (buffer.readableBytes() > 0) {
                values.add(Tdf.read(buffer))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        values
    }

    override fun getTdfByLabel(label: String): Tdf<*>? = content.find { it.label == label }


    override fun toString(): String {
        return "Packet (Component: $component ($rawComponent), Command: $command ($rawCommand), Error; $error, QType: $qtype, Id: $id, Content: [${rawContent.joinToString(", ") { "${it.toInt().and(0xFF)}" }})"
    }
}