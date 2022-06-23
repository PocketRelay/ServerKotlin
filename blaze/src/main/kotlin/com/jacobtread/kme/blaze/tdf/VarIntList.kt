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
        out.writeVarInt(value.size)
        if (value.isNotEmpty()) {
            value.forEach { out.writeVarInt(it) }
        }
    }

    override fun toString(): String = "VarIntList($label: $value)"
}