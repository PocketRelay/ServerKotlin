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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlobTdf) return false
        if (!super.equals(other)) return false
        if (!value.contentEquals(other.value)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}