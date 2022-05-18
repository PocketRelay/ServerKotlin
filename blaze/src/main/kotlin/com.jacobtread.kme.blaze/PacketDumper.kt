package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.*
import com.jacobtread.kme.blaze.utils.VarTripple


fun packetToBuilder(rawPacket: Packet): String {
    val out = StringBuilder("packet(")
    if (rawPacket.command == Command.UNKNOWN
        || rawPacket.component == Component.UNKNOWN
    ) {
        out.append("0x")
            .append(rawPacket.rawComponent.toString(16))
            .append(", 0x")
            .append(rawPacket.rawCommand.toString(16))
    } else {
        out
            .append("Component.")
            .append(rawPacket.component)
            .append(", Command.")
            .append(rawPacket.command)
    }
    out.append(", 0x")
        .append(rawPacket.qtype.toString(16))
        .append(", 0x")
        .append(rawPacket.id.toString(16))

    if (rawPacket.error != 0) {
        out.append(", 0x")
            .append(rawPacket.error.toString(16))
    }

    out.append(") {\n")
    rawPacket.content.forEach {
        appendTdf(out, 1, it, false)
        out.append('\n')
    }
    out.append("}")
    return out.toString()
}

private fun appendTdf(out: StringBuilder, indent: Int, value: Tdf<*>, inline: Boolean) {
    when (value) {
        is VarIntTdf -> {
            out.append("  ".repeat(indent))
                .append("number(\"")
                .append(value.label)
                .append("\", 0x")
                .append(value.value.toString(16))
                .append(")")
        }
        is StringTdf -> {
            out.append("  ".repeat(indent))
                .append("text(\"")
                .append(value.label)
                .append("\", \"")
                .append(value.value)
                .append("\")")
        }
        is BlobTdf -> {
            out.append("  ".repeat(indent))
                .append("blob(\"")
                .append(value.label)
                .append("\", byteArrayOf(")
            val contents = value.value
            val length = contents.size
            for (i in contents.indices) {
                out.append(contents[i].toInt().and(0xFF))
                if (i != length - 1) {
                    out.append(", ")
                }
            }
            out.append("))")
        }
        is StructTdf -> {
            out.append("  ".repeat(indent))
            if (!inline) out.append('+')
            out.append("struct")
            if (value.label.isNotEmpty()) {
                out.append("(\"")
                    .append(value.label)
                    .append("\"")
                if (value.start2) {
                    out.append(", true")
                }
                out.append(") {\n")
            } else {
                if (value.start2) {
                    out.append("(start2=true)")
                }
                out.append(" {\n")
            }
            val contents = value.value
            contents.forEach {
                appendTdf(out, indent + 1, it, false)
                out.append('\n')
            }
            out.append("  ".repeat(indent)).append("}")
        }
        is ListTdf -> {
            out.append("  ".repeat(indent))
                .append("list(\"")
                .append(value.label)
                .append("\", listOf(")

            val content = value.value
            val length = content.size

            when (content[0]) {
                is Long -> {
                    for (i in content.indices) {
                        out.append("0x")
                            .append((content[i] as Long).toString(16))
                        if (i != length - 1) {
                            out.append(", ")
                        }
                    }
                    out.append("))")
                }
                is String -> {
                    for (i in content.indices) {
                        out.append('"')
                            .append(content[i] as String)
                            .append('"')
                        if (i != length - 1) {
                            out.append(", ")
                        }
                    }
                    out.append("))")
                }
                is VarTripple -> {
                    for (i in content.indices) {
                        val tripple = content[i] as VarTripple
                        out.append("VTripple(0x")
                            .append(tripple.a.toString(16))
                            .append(", 0x")
                            .append(tripple.b.toString(16))
                            .append(", 0x")
                            .append(tripple.c.toString(16))
                            .append(")")
                        if (i != length - 1) {
                            out.append(", ")
                        }
                    }
                    out.append("))")
                }
                else -> {
                    out.append('\n')
                    for (i in content.indices) {
                        appendTdf(out, indent + 1, content[i] as StructTdf, true)
                        if (i != length - 1) {
                            out.append(',')
                        }
                        out.append('\n')
                    }
                    out.append("  ".repeat(indent))
                        .append("))")
                }
            }

        }
        is UnionTdf -> {
            val content = value.value
            if (content != null) {
                out.append("  ".repeat(indent))
                    .append("union(\"")
                    .append(value.label)
                    .append("\",\n")
                    .append("  ".repeat(indent))
                    .append("0x")
                    .append(value.type.toString(16))
                    .append(",\n")
                appendTdf(out, indent + 1, content, true)
                out.append("\n")
                    .append("  ".repeat(indent))
                    .append(')')
            } else {
                out.append("  ".repeat(indent))
                    .append("union(\"")
                    .append(value.label)
                    .append("\", 0x")
                    .append(value.type.toString(16))
                    .append(", null)")
            }
        }
        is MapTdf -> {
            val map = value.value
            out.append("  ".repeat(indent))
                .append("map(\"")
                .append(value.label)
                .append("\", mapOf(\n")
            map.entries.forEach { (key, va) ->
                out.append("  ".repeat(indent + 1))
                when (key) {
                    is String -> out.append('"')
                        .append(key)
                        .append('"')
                    is Long -> out.append("0x")
                        .append(key.toString(16))
                }
                out.append(" to ")
                when (va) {
                    is String -> out.append('"')
                        .append(va)
                        .append('"')
                    is Long -> out.append("0x")
                        .append(va.toString(16))
                    is Float -> out.append(va.toString())
                    is StructTdf -> appendTdf(out, indent + 1, va, true)
                }
                out.append(",\n")
            }
            out.append("  ".repeat(indent))
                .append("))")
        }
        is VarIntList -> {
            out.append("  ".repeat(indent))
                .append("varList(\"")
                .append(value.label)
                .append("\", listOf(")
            val contents = value.value
            val length = contents.size
            for (i in contents.indices) {
                out
                    .append("0x")
                    .append(contents[i].toString(16))
                if (i != length - 1) {
                    out.append(", ")
                }
            }
            out.append("))")
        }
        is TrippleTdf -> {
            val trip = value.value
            out.append("  ".repeat(indent))
                .append("tripple(\"")
                .append(value.label)
                .append("\", 0x")
                .append(trip.a.toString(16))
                .append(", 0x")
                .append(trip.b.toString(16))
                .append(", 0x")
                .append(trip.c.toString(16))
                .append(")")
        }
        is PairTdf -> {
            val trip = value.value
            out.append("  ".repeat(indent))
                .append("pair(\"")
                .append(value.label)
                .append("\", 0x")
                .append(trip.a.toString(16))
                .append(", 0x")
                .append(trip.b.toString(16))
                .append(")")
        }
        is FloatTdf -> {
            out.append("  ".repeat(indent))
                .append("float(\"")
                .append(value.label)
                .append("\", ")
                .append(value.toString())
                .append(")")
        }
    }
}
