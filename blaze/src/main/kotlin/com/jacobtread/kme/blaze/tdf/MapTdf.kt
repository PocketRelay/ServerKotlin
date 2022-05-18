package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readString
import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeString
import com.jacobtread.kme.blaze.utils.writeVarInt
import io.netty.buffer.ByteBuf

class MapTdf(
    label: String,
    private val keyType: Int,
    private val valueType: Int,
    override val value: Map<out Any, Any>,
) : Tdf<Map<out Any, Any>>(label, MAP) {

    companion object {
        fun read(label: String, input: ByteBuf): MapTdf {
            val keyType = input.readUnsignedByte().toInt()
            val valueType = input.readUnsignedByte().toInt()
            val count = input.readVarInt().toInt()

            val out = HashMap<Any, Any>()
            repeat(count) {
                val key: Any = when (keyType) {
                    VARINT -> input.readVarInt()
                    STRING -> input.readString()
                    else -> throw IllegalStateException("Unknown list subtype $keyType")
                }
                val value: Any = when (valueType) {
                    VARINT -> input.readVarInt()
                    STRING -> input.readString()
                    STRUCT -> StructTdf.read("", input)
                    FLOAT -> input.readFloat()
                    else -> throw IllegalStateException("Unknown list subtype $keyType")
                }
                out[key] = value
            }
            return MapTdf(label, keyType, valueType, out);
        }
    }

    override fun write(out: ByteBuf) {
        out.writeByte(keyType)
        out.writeByte(valueType)
        val entries = value.entries
        out.writeVarInt(entries.size)
        for ((key, value) in entries) {
            when (keyType) {
                VARINT -> out.writeVarInt(key)
                STRING -> out.writeString(key as String)
                FLOAT -> out.writeFloat(key as Float)
            }
            when (valueType) {
                VARINT -> out.writeVarInt(value)
                STRING -> out.writeString(value as String)
                STRUCT -> (value as StructTdf).write(out)
                FLOAT -> out.writeFloat(value as Float)
            }
        }
    }

    override fun toString(): String = "Map($label: $value)"
}