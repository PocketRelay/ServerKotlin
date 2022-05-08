package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.VPair
import com.jacobtread.kme.utils.VTripple
import io.netty.buffer.ByteBuf

/**
 * TdfBuilder Builder class used to create Tdf structures easily
 * rather than having to create all the objects manually the helper
 * methods on this class can be used instead which automatically adds
 * the correct Tdf value to the values list which is later written
 * as the packet content or used as struct values depending on context
 *
 * @constructor Create empty TdfBuilder
 */
class TdfBuilder {

    val values = ArrayList<Tdf>()

    /**
     * text Adds a new text value to the builder. This
     * becomes a StringTdf when created
     *
     * @param label The label of the Tdf
     * @param value The string value of the Tdf
     */
    fun text(label: String, value: String) {
        values.add(StringTdf(label, value))
    }

    /**
     * number Adds a new number value to the builder.
     * This becomes a VarInt when created
     *
     * @param label The label of the Tdf
     * @param value The long value of the Tdf
     */
    fun number(label: String, value: Long) {
        values.add(VarIntTdf(label, value))
    }

    /**
     * number Adds a new number value to the builder.
     * This becomes a VarInt when created
     *
     * @param label The label of the Tdf
     * @param value The int value of the Tdf
     */
    fun number(label: String, value: Int) {
        values.add(VarIntTdf(label, value.toLong()))
    }

    /**
     * blob Adds a new blob value to the builder.
     * This becomes a BlobTdf when created
     *
     * @param label The label of the Tdf
     * @param value The byte array to be used as the blob
     */
    fun blob(label: String, value: ByteArray) {
        values.add(BlobTdf(label, value))
    }

    /**
     * tripple Adds a new tripple value to the builder.
     * This becomes a TrippleTdf when created
     *
     * @param label The label of the Tdf
     * @param a The first value of the tripple
     * @param b The second value of the tripple
     * @param c The third value of the tripple
     */
    fun tripple(label: String, a: Long, b: Long, c: Long) {
        values.add(TrippleTdf(label, VTripple(a, b, c)))
    }

    /**
     * tripple Adds a new tripple value to the builder.
     * This becomes a TrippleTdf when created
     *
     * @param label The label of the Tdf
     * @param value The tripple value
     */
    fun tripple(label: String, value: VTripple) {
        values.add(TrippleTdf(label, value))
    }

    /**
     * pair Adds a new pair of values to the builder.
     * This becomes a PairTdf when created
     *
     * @param label The label of the Tdf
     * @param a The first value of the pair
     * @param b The second value of the pair
     */
    fun pair(label: String, a: Long, b: Long) {
        values.add(PairTdf(label, VPair(a, b)))
    }

    /**
     * pair Adds a new pair of values to the builder.
     * This becomes a PairTdf when created
     *
     * @param label The label of the Tdf
     * @param value The pair of values
     */
    fun pair(label: String, value: VPair) {
        values.add(PairTdf(label, value))
    }

    /**
     * float Adds a new float value to the builder.
     * This becomes a FloatTdf when created
     *
     * @param label The label of the Tdf
     * @param value The float value
     */
    fun float(label: String, value: Float) {
        values.add(FloatTdf(label, value))
    }

    /**
     * list Adds a new list value to the builder.
     * This becomes a ListTdf when created
     *
     * @param label The label of the Tdf
     * @param value The list value
     */
    fun list(label: String, value: List<Any>) {
        values.add(ListTdf(label, value))
    }

    /**
     * list Adds a new list value to the builder.
     * This becomes a ListTdf when created
     *
     * @param label The label of the Tdf
     * @param values The values to create the list from
     */
    fun list(label: String, vararg values: Any) {
        this.values.add(ListTdf(label, values.toList()))
    }

    /**
     * map Adds a new map value to the builder.
     * This becomes a MapTdf when created
     *
     * @param label The label of the Tdf
     * @param value The map value
     */
    fun map(label: String, value: Map<Any, Any>) {
        values.add(MapTdf(label, value))
    }

    /**
     * union Adds a new union value to the builder.
     * This becomes a UnionTdf when created
     *
     * @param label The label of the Tdf
     * @param type The type of union
     * @param value The value of the union
     */
    fun union(label: String, type: Int = 0x7F, value: Tdf? = null) {
        values.add(UnionTdf(label, type, value))
    }

    /**
     * unaryPlus Overriding the + modifier so that structs can
     * be added to the values using
     * ```
     * +struct("LABEL") {}
     * ```
     */
    operator fun Tdf.unaryPlus() {
        values.add(this)
    }

    /**
     * write Writes all the tdf values to the provided
     * byte buffer
     *
     * @param out The byte buffer to write to
     */
    fun write(out: ByteBuf) {
        values.forEach { it.writeFully(out) }
    }
}

/**
 * struct Creates a new struct tdf element
 *
 * @param label The label of this struct
 * @param start2 Whether the encoded data should start with a byte value of 2
 * @param init Initializer function for setting up this struct
 * @receiver
 * @return The newly created struct
 */
fun struct(label: String = "", start2: Boolean = false, init: TdfBuilder.() -> Unit): StructTdf {
    val context = TdfBuilder()
    context.init()
    return StructTdf(label, start2, context.values)
}

