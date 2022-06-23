package com.jacobtread.kme.blaze.tdf

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

    override fun toString(): String = "VarInt($label: $value)"
}