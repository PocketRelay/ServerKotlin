package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.writeString
import io.netty.buffer.ByteBuf

class StringTdf(label: String, val value: String) : Tdf(label, TDF_STRING) {
    override fun write(byteBuf: ByteBuf) {
        byteBuf.writeString(value)
    }
}