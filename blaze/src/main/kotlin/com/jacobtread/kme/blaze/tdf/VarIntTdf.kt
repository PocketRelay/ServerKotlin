package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.computeVarIntSize
import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import io.netty.buffer.ByteBuf

class VarIntTdf(label: String, override val value: ULong) : Tdf<ULong>(label, VARINT) {
    companion object {
        fun read(label: String, input: ByteBuf): VarIntTdf = VarIntTdf(label, input.readVarInt())
    }

    override fun write(out: ByteBuf) {
        out.writeVarInt(value)
    }

    override fun computeSize(): Int {
        return computeVarIntSize(value)
    }

    override fun toString(): String = "VarInt($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VarIntTdf) return false
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