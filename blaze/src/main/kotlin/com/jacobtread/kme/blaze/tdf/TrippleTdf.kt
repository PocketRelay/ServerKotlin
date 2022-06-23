package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.readVarInt
import com.jacobtread.kme.blaze.utils.writeVarInt
import com.jacobtread.kme.utils.VarTripple
import io.netty.buffer.ByteBuf

class TrippleTdf(label: String, override val value: VarTripple) : Tdf<VarTripple>(label, TRIPPLE) {
    companion object {
        fun read(label: String, input: ByteBuf): TrippleTdf {
            val a = input.readVarInt()
            val b = input.readVarInt()
            val c = input.readVarInt()
            return TrippleTdf(label, VarTripple(a, b, c))
        }
    }

    override fun write(out: ByteBuf) {
        out.writeVarInt(value.a)
        out.writeVarInt(value.b)
        out.writeVarInt(value.c)
    }

    override fun toString(): String = "Tripple($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrippleTdf) return false
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