package com.jacobtread.kme.blaze.utils


fun computeVarIntSize(value: ULong): Int {
    return if (value < 64u) {
        1
    } else {
        var size = 1;
        var curShift = value shr 6
        while (curShift >= 128u) {
            curShift = curShift shr 7
            size++
        }
        size + 1
    }
}

fun computeStringSize(value: String): Int {
    val v = if (value.endsWith(Char.MIN_VALUE)) value else (value + '\u0000')
    val bytes = v.toByteArray(Charsets.UTF_8)
    return computeVarIntSize(bytes.size.toULong()) + bytes.size
}