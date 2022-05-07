package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled
import kotlin.reflect.KClass
import kotlin.reflect.cast


class RawPacket(
    val rawComponent: Int,
    val rawCommand: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val rawContent: ByteArray,
) {
    val component = PacketComponent.from(rawComponent)
    val command = PacketCommand.from(rawComponent, rawCommand)
    val content: List<Tdf> by lazy {
        val buffer = Unpooled.wrappedBuffer(rawContent)
        val values = ArrayList<Tdf>()
        try {
            while (buffer.readableBytes() > 0) {
                values.add(Tdf.read(buffer))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        values
    }

    fun getStringAt(index: Int): String {
        val value = content[index] as StringTdf
        return value.value
    }

    fun <C : Tdf> get(type: KClass<C>, label: String): C? {
        val value = content.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type.java)) return null
        return type.cast(value)
    }

    fun <C : TdfValue<T>, T> getValue(type: KClass<C>, label: String): T? {
        val value = content.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type.java)) return null
        return type.cast(value).value
    }

    override fun toString(): String {
        return "Packet (Component: $component ($rawComponent), Command: $command ($rawCommand), Error; $error, QType: $qtype, Id: $id, Content: [${rawContent.joinToString(", ") { "${it.toInt().and(0xFF)}" }})"
    }

}