package com.jacobtread.kme.blaze.tdf

import io.netty.buffer.ByteBuf

class StringTdf(label: String, override val value: String) : Tdf<String>(label, STRING) {
    companion object {
        fun read(label: String, input: ByteBuf): StringTdf = StringTdf(label, readString(input))
    }

    override fun write(out: ByteBuf) = writeString(out, value)

    override fun computeSize(): Int {
        return computeStringSize(value)
    }

    override fun toString(): String = "String($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringTdf) return false
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