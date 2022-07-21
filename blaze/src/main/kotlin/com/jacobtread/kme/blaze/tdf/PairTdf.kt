package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.data.VarPair
import io.netty.buffer.ByteBuf

class PairTdf(label: String, override val value: VarPair) : Tdf<VarPair>(label, PAIR) {
    companion object {
        fun read(label: String, input: ByteBuf): PairTdf {
            val a = readVarInt(input)
            val b = readVarInt(input)
            return PairTdf(label, VarPair(a, b))
        }
    }

    override fun write(out: ByteBuf) {
        writeVarInt(out, value.a)
        writeVarInt(out, value.b)
    }

    override fun computeSize(): Int {
        return computeVarIntSize(value.a) + computeVarIntSize(value.b)
    }

    override fun toString(): String = "Pair($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PairTdf) return false
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