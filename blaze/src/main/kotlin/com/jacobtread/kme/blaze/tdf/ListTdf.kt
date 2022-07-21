package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.data.VarTripple
import com.jacobtread.kme.blaze.utils.*
import io.netty.buffer.ByteBuf

class ListTdf(label: String, val type: Int, override val value: List<Any>) : Tdf<List<Any>>(label, LIST) {

    companion object {
        fun read(label: String, input: ByteBuf): ListTdf {
            val subType = input.readUnsignedByte().toInt()
            val count = input.readVarInt().toInt()
            return when (subType) {
                VARINT -> {
                    val values = ArrayList<ULong>(count)
                    repeat(count) { values.add(input.readVarInt()) }
                    ListTdf(label, subType, values)
                }
                STRING -> {
                    val values = ArrayList<String>(count)
                    repeat(count) { values.add(input.readString()) }
                    ListTdf(label, subType, values)
                }
                GROUP -> {
                    val values = ArrayList<GroupTdf>(count)
                    repeat(count) { values.add(GroupTdf.read("", input)) }
                    ListTdf(label, subType, values)
                }
                TRIPPLE -> {
                    val values = ArrayList<VarTripple>(count)
                    repeat(count) {
                        values.add(
                            VarTripple(
                                input.readVarInt(),
                                input.readVarInt(),
                                input.readVarInt(),
                            )
                        )
                    }
                    ListTdf(label, subType, values)
                }
                else -> throw IllegalStateException("Unknown list subtype $subType")
            }
        }
    }

    override fun write(out: ByteBuf) {
        out.writeByte(this.type)
        out.writeVarInt(value.size.toULong())
        when (this.type) {
            VARINT -> value.forEach {
                when (it) {
                    is Int -> out.writeVarInt(it.toULong())
                    is Long -> out.writeVarInt(it.toULong())
                    is ULong -> out.writeVarInt(it)
                    is UInt -> out.writeVarInt(it.toULong())
                }
            }
            STRING -> value.forEach { out.writeString(it as String) }
            GROUP -> value.forEach { (it as GroupTdf).write(out) }
            TRIPPLE -> value.forEach {
                val tripple = it as VarTripple
                out.writeVarInt(tripple.a)
                out.writeVarInt(tripple.b)
                out.writeVarInt(tripple.c)
            }
        }
    }

    override fun computeSize(): Int {
        var size = 1 + computeVarIntSize(value.size.toULong())
        when (this.type) {
            VARINT -> value.forEach() {
                when (it) {
                    is Int -> size += computeVarIntSize(it.toULong())
                    is Long -> size += computeVarIntSize(it.toULong())
                    is ULong -> size += computeVarIntSize(it)
                    is UInt -> size += computeVarIntSize(it.toULong())
                }
            }

            STRING -> value.forEach { size += computeStringSize(it as String) }
            GROUP -> value.forEach { size += (it as GroupTdf).computeSize() }
            TRIPPLE -> value.forEach {
                val tripple = it as VarTripple
                size += computeVarIntSize(tripple.a) + computeVarIntSize(tripple.b) + computeVarIntSize(tripple.c)
            }
        }
        return size
    }

    override fun toString(): String = "List($label: $value)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListTdf) return false
        if (!super.equals(other)) return false
        if (type != other.type) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type
        result = 31 * result + value.hashCode()
        return result
    }
}