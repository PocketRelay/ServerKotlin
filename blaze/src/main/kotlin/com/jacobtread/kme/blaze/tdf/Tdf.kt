package com.jacobtread.kme.blaze.tdf

import com.jacobtread.kme.utils.VarTripple
import io.netty.buffer.ByteBuf

abstract class Tdf<V>(val label: String, private val tagType: Int) {
    companion object {
        const val VARINT = 0x0
        const val STRING = 0x1
        const val BLOB = 0x02
        const val GROUP = 0x3
        const val LIST = 0x4
        const val MAP = 0x5
        const val OPTIONAL = 0x6
        const val INT_LIST = 0x7
        const val PAIR = 0x8
        const val TRIPPLE = 0x9
        const val FLOAT = 0xA

        fun getTypeFromClass(valueType: Class<*>): Int {
            return when (valueType) {
                java.lang.Long::class.java,
                java.lang.Integer::class.java,
                -> VARINT
                String::class.java -> STRING
                Float::class.java -> FLOAT
                GroupTdf::class.java -> GROUP
                VarTripple::class.java -> TRIPPLE
                else -> throw IllegalArgumentException("Don't know how to handle type \"${valueType.simpleName}\"")
            }
        }

        fun createTag(labelIn: String): Long {
            val res = IntArray(3)
            val buff = IntArray(4)
            for (i in 0 until 4) {
                if (i >= labelIn.length) {
                    buff[i] = 0
                } else {
                    buff[i] = labelIn[i].code
                }
            }
            res[0] = res[0].or(buff[0].and(0x40).shl(1))
            res[0] = res[0].or(buff[0].and(0x10).shl(2))
            res[0] = res[0].or(buff[0].and(0x0F).shl(2))
            res[0] = res[0].or(buff[1].and(0x40).shr(5))
            res[0] = res[0].or(buff[1].and(0x10).shr(4))

            res[1] = res[1].or(buff[1].and(0x0F).shl(4))
            res[1] = res[1].or(buff[2].and(0x40).shr(3))
            res[1] = res[1].or(buff[2].and(0x10).shr(2))
            res[1] = res[1].or(buff[2].and(0x0C).shr(2))

            res[2] = res[2].or(buff[2].and(0x03).shl(6))
            res[2] = res[2].or(buff[3].and(0x40).shr(1))
            res[2] = res[2].or(buff[3].and(0x1F))

            var tag = 0L
            tag = tag.or(res[0].shl(24).toLong())
            tag = tag.or(res[1].shl(16).toLong())
            tag = tag.or(res[2].shl(8).toLong())
            return tag
        }

        fun createLabel(tag: Long): String {
            val buff = byteArrayOf(
                (tag shr 24).toByte(),
                (tag shr 16).toByte(),
                (tag shr 8).toByte(),
            )
            val res = IntArray(4)
            res[0] = res[0] or (buff[0].toInt() and 0x80 shr 1)
            res[0] = res[0] or (buff[0].toInt() and 0x40 shr 2)
            res[0] = res[0] or (buff[0].toInt() and 0x30 shr 2)
            res[0] = res[0] or (buff[0].toInt() and 0x0C shr 2)

            res[1] = res[1] or (buff[0].toInt() and 0x02 shl 5)
            res[1] = res[1] or (buff[0].toInt() and 0x01 shl 4)
            res[1] = res[1] or (buff[1].toInt() and 0xF0 shr 4)

            res[2] = res[2] or (buff[1].toInt() and 0x08 shl 3)
            res[2] = res[2] or (buff[1].toInt() and 0x04 shl 2)
            res[2] = res[2] or (buff[1].toInt() and 0x03 shl 2)
            res[2] = res[2] or (buff[2].toInt() and 0xC0 shr 6)
            res[3] = res[3] or (buff[2].toInt() and 0x20 shl 1)
            res[3] = res[3] or (buff[2].toInt() and 0x1F)
            val output = StringBuilder()
            for (i in 0..3) {
                if (res[i] != 0) output.append(res[i].toChar())
            }
            return output.toString()
        }

        fun read(input: ByteBuf): Tdf<*> {
            val head = input.readUnsignedInt()
            val tag = (head and 0xFFFFFF00)
            val label = createLabel(tag)
            return when (val type = (head and 0xFF).toInt()) {
                VARINT -> VarIntTdf.read(label, input)
                STRING -> StringTdf.read(label, input)
                BLOB -> BlobTdf.read(label, input)
                GROUP -> GroupTdf.read(label, input)
                LIST -> ListTdf.read(label, input)
                MAP -> MapTdf.read(label, input)
                OPTIONAL -> OptionalTdf.read(label, input)
                INT_LIST -> VarIntList.read(label, input)
                PAIR -> PairTdf.read(label, input)
                TRIPPLE -> TrippleTdf.read(label, input)
                FLOAT -> FloatTdf.read(label, input)
                else -> throw IllegalStateException("Unknown Tdf type: $type")
            }
        }
    }

    abstract val value: V

    abstract fun write(out: ByteBuf)

    fun writeFully(out: ByteBuf) {
        val tag = createTag(label)
        out.writeByte(tag.shr(24).and(0xFF).toInt())
        out.writeByte(tag.shr(16).and(0xFF).toInt())
        out.writeByte(tag.shr(8).and(0xFF).toInt())
        out.writeByte(tagType)
        write(out)
    }
}