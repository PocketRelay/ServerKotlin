package com.jacobtread.kme.blaze.tdf

import io.netty.buffer.ByteBuf

class UnionTdf(label: String, val type: Int = 0x7F, val value: Tdf? = null) : Tdf(label, UNION) {
    companion object {
        fun from(label: String, input: ByteBuf): UnionTdf {
            val type = input.readUnsignedByte().toInt()
            val value = if (type != 0x7F) {
                read(input)
            } else null
            return UnionTdf(label, type, value)
        }
    }

    override fun write(out: ByteBuf) {
        out.writeByte(type)
        if (type != 0x7F) {
            value?.writeFully(out)
        }
    }

    override fun toString(): String = "Union($label: $type, $value)"
}