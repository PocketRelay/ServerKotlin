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

    fun Number(label: String, value: Long): TdfBuilder {
        values.add(VarIntTdf(label, value))
        return this
    }

    fun Number(label: String, value: Int): TdfBuilder {
        values.add(VarIntTdf(label, value.toLong()))
        return this
    }

    fun Number(label: String, value: Short): TdfBuilder {
        values.add(VarIntTdf(label, value.toLong()))
        return this
    }

    fun Blob(label: String, value: ByteArray = ByteArray(0)): TdfBuilder {
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

    fun Struct(label: String = "", start2: Boolean = false, init: TdfBuilder.() -> Unit): TdfBuilder {
        val builder = TdfBuilder()
        builder.init()
        values.add(StructTdf(label, start2, builder.values))
        return this
    }

    fun MakeStruct(label: String = "", start2: Boolean = false, init: TdfBuilder.() -> Unit): StructTdf {
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

    fun Map(label: String, map: Map<Any, Any>): TdfBuilder {
        values.add(MapTdf(label, map))
        return this
    }

    fun Map(label: String, vararg pairs: Pair<Any, Any>): TdfBuilder {
        values.add(MapTdf(label, mapOf(*pairs)))
        return this
    }

    fun Union(label: String, type: Int = 0x7F, value: Tdf? = null): TdfBuilder {
        values.add(UnionTdf(label, type, value))
        return this
    }

    fun write(out: ByteBuf) {
        values.forEach { it.writeFully(out) }
    }
}