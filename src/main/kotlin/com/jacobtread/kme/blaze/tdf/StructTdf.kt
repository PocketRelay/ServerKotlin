package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.writeString
import io.netty.buffer.ByteBuf

class StructTdf(label: String, val values: List<Tdf>, val start2: Boolean) : Tdf(label, TDF_STRUCT) {
    override fun write(byteBuf: ByteBuf) {

    }
}