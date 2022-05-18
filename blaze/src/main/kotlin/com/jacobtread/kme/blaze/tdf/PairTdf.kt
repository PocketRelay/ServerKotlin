package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import com.jacobtread.kme.utils.VPair
import io.netty.buffer.ByteBuf

class PairTdf(label: String, override val value: VPair) : Tdf(label, PAIR), TdfValue<VPair> {
    companion object {
        fun from(label: String, input: ByteBuf): PairTdf {
            val a = input.readVarInt()
            val b = input.readVarInt()
            return PairTdf(label, VPair(a, b))
        }
    }

    override fun write(out: ByteBuf) {
        out.writeVarInt(value.a)
        out.writeVarInt(value.b)
    }

    override fun toString(): String = "Pair($label: $value)"
}