package com.jacobtread.kme.blaze.tdf

import io.netty.buffer.ByteBuf


abstract class Tdf(val label: String, val type: Byte) {

    companion object {
        const val TDF_VAR_INT: Byte = 0x0
        const val TDF_STRING: Byte = 0x1
        const val TDF_BLOB: Byte = 0x02
        const val TDF_STRUCT: Byte = 0x3
        const val TDF_LIST: Byte = 0x4
        const val TDF_DOUBLE_LIST: Byte = 0x05
        const val TDF_UNION: Byte = 0x06
        const val TDF_INT_LIST: Byte = 0x07
        const val TDF_TRIPPLE: Byte = 0x09
        const val TDF_FLOAT: Byte = 0xA
    }

    abstract fun write(byteBuf: ByteBuf)
}