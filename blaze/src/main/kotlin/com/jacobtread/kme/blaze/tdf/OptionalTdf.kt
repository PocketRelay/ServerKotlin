package com.jacobtread.kme.blaze.tdf

import io.netty.buffer.ByteBuf

class OptionalTdf(label: String, val type: Int = 0x7F, override val value: Tdf<*>? = null) : Tdf<Tdf<*>?>(label, OPTIONAL) {
    companion object {
        fun read(label: String, input: ByteBuf): OptionalTdf {
            val type = input.readUnsignedByte().toInt()
            val value = if (type != 0x7F) {
                read(input)
            } else null
            return OptionalTdf(label, type, value)
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