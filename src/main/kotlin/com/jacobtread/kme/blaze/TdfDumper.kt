package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.VTripple
import com.jacobtread.kme.utils.hex

object TdfDumper {

    private const val STRUCT_INDENT = 2

    fun dump(values: List<Tdf>, indent: Int = 0, inline: Boolean = false): String {
        val out = StringBuilder()
        for (value in values) {
            out.append(dump(value, indent, inline))
            if (value is StructTdf) {
                out.append('\n')
            }
        }
        return out.toString()
    }

    private fun dump(value: Tdf, indent: Int = 0, inline: Boolean): String {
        val label = value.label
        return " ".repeat(indent) + when (value) {
            is VarIntTdf -> "$label: ${value.value.hex()}"
            is StringTdf -> "$label: \"${value.value}\""
            is BlobTdf -> "$label: BLOB(" + value.value.joinToString(", ") { it.toInt().and(0xFF).toString() } + ")"
            is StructTdf -> dumpStruct(value, indent, inline)
            is ListTdf -> "$label: [" + value.value.joinToString(", ") { dumpListValue(it, indent, inline) } + "]"
            is UnionTdf -> dumpUnion(value, indent, inline)
            is VarIntList -> "$label: [" + value.value.joinToString(", ") { it.toString() } + "]"
            is PairListTdf -> dumpPairList(value, indent, inline)
            is PairTdf -> "(${value.value.a.hex()}, ${value.value.b.hex()})"
            is TrippleTdf -> "(${value.value.a.hex()}, ${value.value.b.hex()}, ${value.value.c.hex()})"
            is FloatTdf -> "${value.value}"
            else -> value.toString()
        }
    }

    private fun dumpStruct(value: StructTdf, indent: Int, inline: Boolean): String {
        val builder = StringBuilder()
        val newIndent = indent + STRUCT_INDENT
        builder.append(value.label)
            .append(" {")
        if (inline) {
            builder.append(' ')
            value.value.forEach {
                builder.append(dump(it, newIndent, true))
                builder.append(", ")
            }
            builder.append('}')
        } else {
            builder.append('\n')
            value.value.forEach {
                builder.append(" ".repeat(newIndent))
                builder.append(dump(it, newIndent, false))
                builder.append('\n')
            }
            builder.append(" ".repeat(indent) + "}")
        }
        return builder.toString()
    }

    private fun dumpUnion(value: UnionTdf, indent: Int, inline: Boolean): String {
        val builder = StringBuilder()
        builder.append(value.label)
            .append(" (")
            .append(value.type.hex())
            .append("): ")
        if (value.type == 0x7F) {
            builder.append("Empty")
        } else {
            builder.append(dump(value, indent, inline))
        }
        return builder.toString()
    }

    private fun dumpListValue(value: Any, indent: Int, inline: Boolean): String {
        return when (value) {
            is Long -> value.hex()
            is String -> "\"$value\""
            is StructTdf -> dumpStruct(value, indent, inline)
            is VTripple -> "(${value.a.hex()}, ${value.b.hex()}, ${value.c.hex()})"
            else -> value.toString()
        }
    }

    private fun dumpPairList(value: PairListTdf, indent: Int, inline: Boolean): String {
        val builder = StringBuilder()
        builder.append(value.label)
            .append(": [")
        val a = value.a
        val b = value.b
        if (inline) {
            builder.append(' ')
            for (i in a.indices) {
                builder.append('(')
                    .append(dumpListValue(a[i], indent, true))
                    .append(',')
                    .append(dumpListValue(b[i], indent, true))
                    .append("), ")
            }
            builder.append("]")
        } else {
            builder.append('\n')
            for (i in a.indices) {
                builder.append('(')
                    .append(dumpListValue(a[i], indent, false))
                    .append(',')
                    .append(dumpListValue(b[i], indent, false))
                    .append("),\n")
            }
            builder.append("]")
        }
        return builder.toString()
    }

}