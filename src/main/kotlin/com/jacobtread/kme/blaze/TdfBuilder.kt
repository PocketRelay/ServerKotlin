package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.VPair
import com.jacobtread.kme.utils.VTripple
import io.netty.buffer.ByteBuf

class TdfBuilder {

    val values = ArrayList<Tdf>()

    fun text(label: String, value: String) {
        values.add(StringTdf(label, value))
    }

    fun number(label: String, value: Long) {
        values.add(VarIntTdf(label, value))
    }

    fun number(label: String, value: Int) {
        values.add(VarIntTdf(label, value.toLong()))
    }

    fun blob(label: String, value: ByteArray) {
        values.add(BlobTdf(label, value))
    }

    fun tripple(label: String, a: Long, b: Long, c: Long) {
        values.add(TrippleTdf(label, VTripple(a, b, c)))
    }

    fun tripple(label: String, value: VTripple) {
        values.add(TrippleTdf(label, value))
    }

    fun pair(label: String, a: Long, b: Long) {
        values.add(PairTdf(label, VPair(a, b)))
    }

    fun pair(label: String, value: VPair) {
        values.add(PairTdf(label, value))
    }

    fun float(label: String, value: Float) {
        values.add(FloatTdf(label, value))
    }

    fun list(label: String, value: List<Any>) {
        values.add(ListTdf(label, value))
    }

    fun map(label: String, value: Map<Any, Any>) {
        values.add(MapTdf(label, value))
    }

    fun union(label: String, type: Int = 0x7F, value: Tdf? = null) {
        values.add(UnionTdf(label, type, value))
    }

    operator fun Tdf.unaryPlus() {
        values.add(this)
    }

    fun write(out: ByteBuf) {
        values.forEach { it.writeFully(out) }
    }
}

fun struct(label: String = "", start2: Boolean = false, init: TdfBuilder.() -> Unit): StructTdf {
    val context = TdfBuilder()
    context.init()
    return StructTdf(label, start2, context.values)
}

