package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readString
import com.jacobtread.kme.blaze.utils.writeString
import io.netty.buffer.ByteBuf

class StringTdf(label: String, override val value: String) : Tdf(label, STRING), TdfValue<String> {
    companion object {
        fun from(label: String, input: ByteBuf): StringTdf {
            return StringTdf(label, input.readString())
        }
    }

    override fun write(out: ByteBuf) = out.writeString(value)
    override fun toString(): String = "String($label: $value)"
}