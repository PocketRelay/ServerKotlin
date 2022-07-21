package com.jacobtread.kme.blaze.tdf

import io.netty.buffer.ByteBuf

class OptionalTdf(label: String, val type: Int = 0x7F, override val value: Tdf<*>? = null) : Tdf<Tdf<*>?>(label, OPTIONAL) {
    companion object {
        fun read(label: String, input: ByteBuf): OptionalTdf {
            val type = input.readUnsignedByte().toInt()
            val value = if (type != 0x7F) {
                read(input)
            } else null
            return OptionalTdf(label, type, value)
        }
    }

    override fun write(out: ByteBuf) {
        out.writeByte(type)
        if (type != 0x7F) {
            value?.writeFully(out)
        }
    }

    override fun computeSize(): Int {
        return if (type != 0x7F && value != null) {
            value.computeFullSize() + 1
        } else {
            1
        }
    }

    override fun toString(): String = "Optional($label: $type, $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OptionalTdf) return false
        if (!super.equals(other)) return false
        if (type != other.type) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}