package com.jacobtread.kme.blaze.utils

import com.jacobtread.kme.utils.BigEndian

object Labels {

    fun toTag(labelIn: String): Long {
        var label = labelIn.padEnd(4, Char.MIN_VALUE)
        if (label.length > 4) label = label.substring(0, 4)
        val res = IntArray(3)
        val buff = IntArray(4) { label[it].code }
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

        var tag = 0L.or(res[0].shl(24).toUInt().toLong())
        tag = tag.or(res[1].shl(16).toUInt().toLong())
        tag = tag.or(res[2].shl(8).toUInt().toLong())
        return tag
    }

    fun fromTag(tag: Long): String {
        val buff = BigEndian.uint32ToBytes(tag)
        val res = IntArray(4)
        res[0] = (res[0] or (buff[0].toInt() and 0x80 shr 1)) and 0xFF
        res[0] = (res[0] or (buff[0].toInt() and 0x40 shr 2)) and 0xFF
        res[0] = (res[0] or (buff[0].toInt() and 0x30 shr 2)) and 0xFf
        res[0] = (res[0] or (buff[0].toInt() and 0x0C shr 2)) and 0xFF

        res[1] = (res[1] or (buff[0].toInt() and 0x02 shl 5)) and 0xFF
        res[1] = (res[1] or (buff[0].toInt() and 0x01 shl 4)) and 0xFF
        res[1] = (res[1] or (buff[1].toInt() and 0xF0 shr 4)) and 0xFF

        res[2] = (res[2] or (buff[1].toInt() and 0x08 shl 3)) and 0xFF
        res[2] = (res[2] or (buff[1].toInt() and 0x04 shl 2)) and 0xFF
        res[2] = (res[2] or (buff[1].toInt() and 0x03 shl 2)) and 0xFF
        res[2] = (res[2] or (buff[2].toInt() and 0xC0 shr 6)) and 0xFF
        res[3] = (res[3] or (buff[2].toInt() and 0x20 shl 1)) and 0xFf
        res[3] = (res[3] or (buff[2].toInt() and 0x1F)) and 0xFF
        val output = StringBuilder()
        for (value in res) {
            if (value != 0) output.append(value.toChar())
        }
        return output.toString()
    }
}