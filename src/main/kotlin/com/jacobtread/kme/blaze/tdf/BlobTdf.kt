package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.writeVarInt
import io.netty.buffer.ByteBuf

class BlobTdf(label: String, val value: ByteArray) : Tdf(label, TDF_BLOB) {
    override fun write(byteBuf: ByteBuf) {
        byteBuf.writeVarInt(value.size.toLong())
        byteBuf.writeBytes(value)
    }
}