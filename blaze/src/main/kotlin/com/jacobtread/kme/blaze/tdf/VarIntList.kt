package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import io.netty.buffer.ByteBuf

class VarIntList(label: String, override val value: List<ULong>) : Tdf<List<ULong>>(label, INT_LIST) {
    companion object {
        fun read(label: String, input: ByteBuf): VarIntList {
            val count = input.readVarInt().toInt()
            val values = ArrayList<ULong>(count)
            repeat(count) { values.add(input.readVarInt()) }
            return VarIntList(label, values)
        }
    }

    override fun write(out: ByteBuf) {
        out.writeVarInt(value.size.toULong())
        if (value.isNotEmpty()) {
            value.forEach { out.writeVarInt(it) }
        }
    }

    override fun computeSize(): Int {
        var size = computeVarIntSize(value.size.toULong())
        if (value.isNotEmpty()) {
            value.forEach { size += computeVarIntSize(it) }
        }
        return size
    }

    override fun toString(): String = "VarIntList($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VarIntList) return false
        if (!super.equals(other)) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}