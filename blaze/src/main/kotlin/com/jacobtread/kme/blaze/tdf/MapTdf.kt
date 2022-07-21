package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.*
import io.netty.buffer.ByteBuf

class MapTdf(
    label: String,
    private val keyType: Int,
    private val valueType: Int,
    override val value: Map<*, *>,
) : Tdf<Map<*, *>>(label, MAP) {

    companion object {
        fun read(label: String, input: ByteBuf): MapTdf {
            val keyType = input.readUnsignedByte().toInt()
            val valueType = input.readUnsignedByte().toInt()
            val count = input.readVarInt().toInt()
            val out = LinkedHashMap<Any, Any>()
            repeat(count) {
                val key: Any = when (keyType) {
                    VARINT -> input.readVarInt()
                    STRING -> input.readString()
                    else -> throw IllegalStateException("Unknown list subtype $keyType")
                }
                val value: Any = when (valueType) {
                    VARINT -> input.readVarInt()
                    STRING -> input.readString()
                    GROUP -> GroupTdf.read("", input)
                    FLOAT -> input.readFloat()
                    else -> throw IllegalStateException("Unknown list subtype $keyType")
                }
                out[key] = value
            }
            return MapTdf(label, keyType, valueType, out)
        }
    }

    override fun write(out: ByteBuf) {
        out.writeByte(keyType)
        out.writeByte(valueType)
        val entries = value.entries
        out.writeVarInt(entries.size.toULong())
        for ((key, value) in entries) {
            when (keyType) {
                VARINT -> {
                    when (key) {
                        is Int -> out.writeVarInt(key.toULong())
                        is Long -> out.writeVarInt(key.toULong())
                        is ULong -> out.writeVarInt(key)
                        is UInt -> out.writeVarInt(key.toULong())
                    }
                }
                STRING -> out.writeString(key as String)
                FLOAT -> out.writeFloat(key as Float)
            }
            when (valueType) {
                VARINT -> {
                    when (value) {
                        is Int -> out.writeVarInt(value.toULong())
                        is Long -> out.writeVarInt(value.toULong())
                        is ULong -> out.writeVarInt(value)
                        is UInt -> out.writeVarInt(value.toULong())
                    }
                }
                STRING -> out.writeString(value as String)
                GROUP -> (value as GroupTdf).write(out)
                FLOAT -> out.writeFloat(value as Float)
            }
        }
    }

    override fun computeSize(): Int {
        val entries = value.entries
        var size = 2 + computeVarIntSize(entries.size.toULong())
        for ((key, value) in entries) {
            when (keyType) {
                VARINT -> {
                    when (key) {
                        is Int -> size += computeVarIntSize(key.toULong())
                        is Long -> size += computeVarIntSize(key.toULong())
                        is ULong -> size += computeVarIntSize(key)
                        is UInt -> size += computeVarIntSize(key.toULong())
                    }
                }
                STRING -> size += computeStringSize(key as String)
                FLOAT -> size += 4
            }
            when (valueType) {
                VARINT -> {
                    when (value) {
                        is Int -> size += computeVarIntSize(value.toULong())
                        is Long -> size += computeVarIntSize(value.toULong())
                        is ULong -> size += computeVarIntSize(value)
                        is UInt -> size += computeVarIntSize(value.toULong())
                    }
                }
                STRING -> size += computeStringSize(value as String)
                GROUP -> size += (value as GroupTdf).computeSize()
                FLOAT -> size += 4
            }
        }
        return size
    }

    override fun toString(): String = "Map($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapTdf) return false
        if (!super.equals(other)) return false
        if (keyType != other.keyType) return false
        if (valueType != other.valueType) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + keyType
        result = 31 * result + valueType
        result = 31 * result + value.hashCode()
        return result
    }
}