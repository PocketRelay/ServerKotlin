package com.jacobtread.kme.utils

object Labels {

    fun toTag(labelIn: String): Int {
        var label = labelIn
        label.padEnd(4, Char.MIN_VALUE)
        if (label.length > 4) label = label.substring(0, 4)
        val res = IntArray(3)
        val buff = IntArray(4) { label[it].code }
        var tag = 0;
        res[0] = res[0] or ((buff[0] and 0x40) shl 1)
        res[0] = res[0] or ((buff[0] and 0x10) shl 2)
        res[0] = res[0] or ((buff[0] and 0x0F) shl 2)
        res[0] = res[0] or ((buff[1] and 0x40) shr 5)
        tag = tag or ((res[0] or ((buff[1] and 0x10) shr 4)) shl 24)
        res[1] = res[1] or ((buff[1] and 0x0F) shl 4)
        res[1] = res[1] or ((buff[2] and 0x40) shr 3)
        res[1] = res[1] or ((buff[2] and 0x10) shr 2)
        tag = tag or ((res[1] or ((buff[2] and 0x0C) shr 2)) shl 16)
        res[2] = res[2] or ((buff[2] and 0x03) shl 6)
        res[2] = res[2] or ((buff[3] and 0x40) shr 1)
        tag = tag or ((res[2] or ((buff[3] and 0x1F))) shl 8)
        return tag
    }

    fun fromTag(tag: Int): String {
        val buff = tag.asBEBytes()
        val res = ByteArray(4)
        res[0] = (res[0].toInt() or (buff[0].toInt() and 0x80 shr 1)).toByte()
        res[0] = (res[0].toInt() or (buff[0].toInt() and 0x40 shr 2)).toByte()
        res[0] = (res[0].toInt() or (buff[0].toInt() and 0x30 shr 2)).toByte()
        res[0] = (res[0].toInt() or (buff[0].toInt() and 0x0C shr 2)).toByte()
        res[1] = (res[1].toInt() or (buff[0].toInt() and 0x02 shl 5)).toByte()
        res[1] = (res[1].toInt() or (buff[0].toInt() and 0x01 shl 4)).toByte()
        res[1] = (res[1].toInt() or (buff[1].toInt() and 0xF0 shr 4)).toByte()
        res[2] = (res[2].toInt() or (buff[1].toInt() and 0x08 shl 3)).toByte()
        res[2] = (res[2].toInt() or (buff[1].toInt() and 0x04 shl 2)).toByte()
        res[2] = (res[2].toInt() or (buff[1].toInt() and 0x03 shl 2)).toByte()
        res[2] = (res[2].toInt() or (buff[2].toInt() and 0xC0 shr 6)).toByte()
        res[3] = (res[3].toInt() or (buff[2].toInt() and 0x20 shl 1)).toByte()
        res[3] = (res[3].toInt() or (buff[2].toInt() and 0x1F)).toByte()
        val output = StringBuilder()
        for (i in 0..3) {
            if (res[i].toInt() == 0) res[i] = 0x20
            output.append(res[i].toInt().toChar())
        }
        return output.toString()
    }
}