package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.*
import io.netty.buffer.ByteBuf
import kotlin.reflect.KClass
import kotlin.reflect.cast

class ListTdf(label: String, val type: Int, override val value: List<Any>) : Tdf(label, LIST), TdfValue<List<Any>> {

    companion object {
        fun from(label: String, input: ByteBuf): ListTdf {
            val subType = input.readUnsignedByte().toInt()
            val count = input.readVarInt().toInt()
            return when (subType) {
                VARINT -> {
                    val values = ArrayList<Long>(count)
                    repeat(count) { values.add(input.readVarInt()) }
                    ListTdf(label, subType, values)
                }
                STRING -> {
                    val values = ArrayList<String>(count)
                    repeat(count) { values.add(input.readString()) }
                    ListTdf(label, subType, values)
                }
                STRUCT -> {
                    val values = ArrayList<StructTdf>(count)
                    repeat(count) { values.add(StructTdf.from("", input)) }
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
        out.writeVarInt(value.size)
        when (this.type) {
            VARINT -> value.forEach { out.writeVarInt(it) }
            STRING -> value.forEach { out.writeString(it as String) }
            STRUCT -> value.forEach { (it as StructTdf).write(out) }
            TRIPPLE -> value.forEach {
                val tripple = it as VarTripple
                out.writeVarInt(tripple.a)
                out.writeVarInt(tripple.b)
                out.writeVarInt(tripple.c)
            }
        }
    }

    override fun toString(): String = "List($label: $value)"

    fun <T : Any> getAtIndex(type: KClass<T>, index: Int): T? {
        val value = value.getOrNull(index) ?: return null
        if (!value.javaClass.isAssignableFrom(type.java)) return null
        return type.cast(value)
    }
}