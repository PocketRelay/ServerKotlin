package com.jacobtread.kme.blaze.builder

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.utils.VPair
import com.jacobtread.kme.utils.VTripple
import io.netty.buffer.ByteBuf

class TdfBuilder {

    val values = ArrayList<Tdf>()

    fun Text(label: String, value: String): TdfBuilder {
        values.add(StringTdf(label, value))
        return this
    }

    fun VarInt(label: String, value: Long): TdfBuilder {
        values.add(VarIntTdf(label, value))
        return this
    }

    fun VarInt(label: String, value: Int): TdfBuilder {
        values.add(VarIntTdf(label, value.toLong()))
        return this
    }

    fun VarInt(label: String, value: Short): TdfBuilder {
        values.add(VarIntTdf(label, value.toLong()))
        return this
    }

    fun Blob(label: String, value: ByteArray): TdfBuilder {
        values.add(BlobTdf(label, value))
        return this
    }

    fun Trpple(label: String, a: Long, b: Long, c: Long): TdfBuilder {
        values.add(TrippleTdf(label, VTripple(a, b, c)))
        return this
    }

    fun Trpple(label: String, value: VTripple): TdfBuilder {
        values.add(TrippleTdf(label, value))
        return this
    }

    fun Pair(label: String, a: Long, b: Long): TdfBuilder {
        values.add(PairTdf(label, VPair(a, b)))
        return this
    }

    fun Pair(label: String, value: VPair): TdfBuilder {
        values.add(PairTdf(label, value))
        return this
    }

    fun Float(label: String, value: Float): TdfBuilder {
        values.add(FloatTdf(label, value))
        return this
    }

    fun Struct(label: String, start2: Boolean = false, init: TdfBuilder.() -> Unit): TdfBuilder {
        val builder = TdfBuilder()
        builder.init()
        values.add(StructTdf(label, start2, builder.values))
        return this
    }

    fun StructInline(label: String, start2: Boolean = false, init: TdfBuilder.() -> Unit): StructTdf {
        val builder = TdfBuilder()
        builder.init()
        return StructTdf(label, start2, builder.values)
    }

    fun Struct(label: String, start2: Boolean = false, values: List<Tdf>): TdfBuilder {
        this.values.add(StructTdf(label, start2, values))
        return this
    }

    fun List(label: String, values: List<Any>): TdfBuilder {
        this.values.add(ListTdf(label, values))
        return this
    }

    fun PairList(label: String, a: List<Any>, b: List<Any>): TdfBuilder {
        values.add(PairListTdf(label, a, b))
        return this
    }

    fun Union(label: String, type: Int = 0x7F, value: Tdf? = null): TdfBuilder {
        values.add(UnionTdf(label, type, value))
        return this
    }

    fun write(out: ByteBuf) {
        for (value in values) {
            value.writeHead(out)
            value.write(out)
        }
    }
}