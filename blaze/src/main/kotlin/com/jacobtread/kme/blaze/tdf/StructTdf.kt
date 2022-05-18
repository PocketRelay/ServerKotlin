package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.TdfContainer
import io.netty.buffer.ByteBuf

class StructTdf(label: String, val start2: Boolean, override val value: List<Tdf<*>>) : Tdf<List<Tdf<*>>>(label, STRUCT), TdfContainer {
    companion object {
        fun read(label: String, input: ByteBuf): StructTdf {
            val out = ArrayList<Tdf<*>>()
            var start2 = false
            var byte: Int
            while (true) {
                byte = input.readUnsignedByte().toInt()
                if (byte == 0) break
                if (byte == 2) {
                    start2 = true
                } else {
                    input.readerIndex(input.readerIndex() - 1)
                }
                out.add(read(input))
            }
            return StructTdf(label, start2, out)
        }
    }

    override fun write(out: ByteBuf) {
        if (start2) out.writeByte(2)
        value.forEach {
            it.writeFully(out)
        }
        out.writeByte(0)
    }

    override fun getTdfByLabel(label: String): Tdf<*>? = value.find { it.label == label }
    override fun toString(): String = "Struct($label: $value)"
}