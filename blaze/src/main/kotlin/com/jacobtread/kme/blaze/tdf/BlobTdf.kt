package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import io.netty.buffer.ByteBuf

class BlobTdf(label: String, override val value: ByteArray) : Tdf<ByteArray>(label, BLOB) {
    companion object {
        fun read(label: String, input: ByteBuf): BlobTdf {
            val size = input.readVarInt().toInt()
            val byteArray = ByteArray(size)
            if (size > 0) input.readBytes(byteArray)
            return BlobTdf(label, byteArray)
        }
    }

    override fun write(out: ByteBuf) {
        out.writeVarInt(value.size)
        if (value.isNotEmpty()) {
            out.writeBytes(value)
        }
    }

    override fun toString(): String = "Blob($label: ${value.contentToString()})"
}