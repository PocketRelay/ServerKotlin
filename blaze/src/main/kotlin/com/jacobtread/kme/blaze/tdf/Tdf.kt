package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.blaze.utils.Labels
import com.jacobtread.kme.blaze.utils.VarTripple
import io.netty.buffer.ByteBuf

abstract class Tdf(val label: String, private val tagType: Int) {
    companion object {
        const val VARINT = 0x0
        const val STRING = 0x1
        const val BLOB = 0x02
        const val STRUCT = 0x3
        const val LIST = 0x4
        const val MAP = 0x5
        const val UNION = 0x6
        const val INT_LIST = 0x7
        const val PAIR = 0x8
        const val TRIPPLE = 0x9
        const val FLOAT = 0xA

        fun getTypeFromClass(valueType: Class<*>): Int {
            return when (valueType) {
                Long::class.java,
                Int::class.java, -> VARINT
                String::class.java -> STRING
                Float::class.java -> FLOAT
                StructTdf::class.java -> STRUCT
                VarTripple::class.java -> TRIPPLE
                else -> throw IllegalArgumentException("Don't know how to handle type \"${valueType.simpleName}")
            }
        }

        fun read(input: ByteBuf): Tdf {
            val head = input.readUnsignedInt()
            val tag = (head and 0xFFFFFF00)
            val label = Labels.fromTag(tag).trimEnd()
            return when (val type = (head and 0xFF).toInt()) {
                VARINT -> VarIntTdf.from(label, input)
                STRING -> StringTdf.from(label, input)
                BLOB -> BlobTdf.from(label, input)
                STRUCT -> StructTdf.from(label, input)
                LIST -> ListTdf.from(label, input)
                MAP -> MapTdf.from(label, input)
                UNION -> UnionTdf.from(label, input)
                INT_LIST -> VarIntList.from(label, input)
                PAIR -> PairTdf.from(label, input)
                TRIPPLE -> TrippleTdf.from(label, input)
                FLOAT -> FloatTdf.from(label, input)
                else -> throw IllegalStateException("Unknown Tdf type: $type")
            }
        }
    }

    abstract fun write(out: ByteBuf)

    fun writeFully(out: ByteBuf) {
        val tag = Labels.toTag(label)
        out.writeByte(tag.shr(24).and(0xFF).toInt())
        out.writeByte(tag.shr(16).and(0xFF).toInt())
        out.writeByte(tag.shr(8).and(0xFF).toInt())
        out.writeByte(tagType)
        write(out)
    }
}