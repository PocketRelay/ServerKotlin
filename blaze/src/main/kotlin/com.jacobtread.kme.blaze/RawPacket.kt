package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.exception.InvalidTdfException
import io.netty.buffer.ByteBuf
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
    companion object {
        fun read(input: ByteBuf): RawPacket {
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
            return RawPacket(component, command, error, qtype, id, content)
        }
    }


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

    @Throws(InvalidTdfException::class)
    fun <C : TdfValue<T>, T> getValue(type: KClass<C>, label: String): T {
        val value = content.find { it.label == label } ?: throw InvalidTdfException(label, "No value found")
        if (!value.javaClass.isAssignableFrom(type.java)) throw InvalidTdfException(label, "Value not of type: ${value.javaClass.simpleName}")
        try {
            return type.cast(value).value
        } catch (e: ClassCastException) {
            throw InvalidTdfException(label, "Failed to cast value to: ${value.javaClass.simpleName}")
        }
    }

    fun <C : TdfValue<T>, T> getValueOrNull(type: KClass<C>, label: String): T? {
        val value = content.find { it.label == label }
        if (value == null || !value.javaClass.isAssignableFrom(type.java)) return null
        return try {
            type.cast(value).value
        } catch (e: ClassCastException) {
            null
        }
    }

    override fun toString(): String {
        return "Packet (Component: $component ($rawComponent), Command: $command ($rawCommand), Error; $error, QType: $qtype, Id: $id, Content: [${rawContent.joinToString(", ") { "${it.toInt().and(0xFF)}" }})"
    }

    fun toDebugString(raw: Boolean = false): String {
        val builder = StringBuilder()
        builder.apply {
            append("====== Packet Dump ======\n")
            append("Component: $component (9x${rawComponent.toString(16)})\n")
            append("Command: $command (0x${rawCommand.toString(16)})\n")
            append("Error: 0x${error.toString(16)}\n")
            append("QType: 0x${qtype.toString(16)}\n")
            append("ID: 0x${id.toString(16)}\n")
            val content = content
            append("Raw Content Length: ${content.size}\n")
            if (raw) {
                append('[')
                rawContent.forEach {
                    append(it.toInt().and(0xFF))
                    append(", ")
                }
                append("]\n")
            }
            append("Content Length: ${content.size}\n")
            append("=== Content ==\n")
            append(TdfDumper.dump(content))
            append("==== End Packet Dump ====\n")
        }
        return builder.toString()
    }

}