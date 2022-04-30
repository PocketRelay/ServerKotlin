package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.writeVarInt
import io.netty.buffer.ByteBuf

class VarIntTdf(label: String, val value: Long) : Tdf(label, TDF_VAR_INT) {
    override fun write(byteBuf: ByteBuf) {
        byteBuf.writeVarInt(value)
    }
}