package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

data class Tdf<V>(val label: String, val value: V)
class TdfStruct(val start2: Boolean) : TdfGroup()
data class ListPair(val a: List<*>, val b: List<*>)

open class TdfGroup {
    val contents = ArrayList<Tdf<*>>()

    companion object {
        private const val VAR_INT = 0x0
        private const val STRING = 0x1
        private const val BLOB = 0x02
        private const val STRUCT = 0x3
        private const val LIST = 0x4
        private const val PAIR_LIST = 0x05
        private const val UNION = 0x06
        private const val INT_LIST = 0x07
        private const val PAIR = 0x08
        private const val TRIPPLE = 0x09
        private const val FLOAT = 0xA

        private const val VAR_LIST = 0x0
        private const val STR_LIST = 0x1
        private const val STRUCT_LIST = 0x3
        private const val TRIPPLE_LIST = 0x9
        private const val FLOAT_LIST = 0xA
    }

    fun Text(label: String, value: String): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun VarInt(label: String, value: Long): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun Blob(label: String, value: ByteArray): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun Tripple(label: String, a: Long, b: Long, c: Long): TdfGroup {
        contents.add(Tdf(label, Tripple(a, b, c)))
        return this
    }

    fun Tripple(label: String, value: Tripple): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun Pair(label: String, a: Long, b: Long): TdfGroup {
        contents.add(Tdf(label, Pair(a, b)))
        return this
    }

    fun Pair(label: String, value: Pair): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun Float(label: String, value: Float): TdfGroup {
        contents.add(Tdf(label, value))
        return this
    }

    fun Struct(label: String, start2: Boolean, init: TdfGroup.() -> Unit): TdfGroup {
        val group = TdfStruct(start2)
        group.init()
        contents.add(Tdf(label, group))
        return this
    }

    fun List(label: String, values: List<*>): TdfGroup {
        contents.add(Tdf(label, values))
        return this
    }

    fun ListPair(label: String, a: List<*>, b: List<*>): TdfGroup {
        contents.add(Tdf(label, ListPair(a, b)))
        return this
    }

    fun write(out: ByteBuf, writeTag: Boolean) {
        for (content in contents) {
            if (writeTag) {
                val tag = Labels.toTag(content.label)
                out.writeByte(tag shr 24)
                out.writeByte(tag shr 16)
                out.writeByte(tag shr 8)
            }
            when (val value = content.value) {
                is Long -> {
                    if (writeTag) out.writeByte(VAR_INT)
                    out.writeVarInt(value)
                }
                is String -> {
                    if (writeTag) out.writeByte(STRING)
                    out.writeString(value)
                }
                is ByteArray -> {
                    if (writeTag) out.writeByte(BLOB)
                    out.writeVarInt(value.size.toLong())
                    out.writeBytes(value)
                }
                is Tripple -> {
                    if (writeTag) out.writeByte(TRIPPLE)
                    out.writeVarInt(value.a)
                    out.writeVarInt(value.b)
                    out.writeVarInt(value.c)
                }
                is Pair -> {
                    if (writeTag) out.writeByte(PAIR)
                    out.writeVarInt(value.a)
                    out.writeVarInt(value.b)
                }
                is Float -> {
                    if (writeTag) out.writeByte(FLOAT)
                    out.writeFloat(value)
                }
                is TdfStruct -> {
                    if (writeTag) out.writeByte(STRUCT)
                    if (value.start2) out.writeByte(2)
                    value.write(out, true)
                    out.writeByte(0)
                }
                is List<*> -> {
                    if (writeTag) out.writeByte(LIST)
                    out.writeByte(listType(value))
                    value.forEach { writeListElement(it, out) }
                }
                is ListPair -> {
                    if (writeTag) out.writeByte(PAIR_LIST)
                    val a = value.a
                    val b = value.b
                    out.writeByte(listType(a))
                    out.writeByte(listType(b))
                    for (i in a.indices) {
                        writeListElement(a[i], out)
                        writeListElement(b[i], out)
                    }
                }
            }
        }
    }

    fun listType(list: List<*>): Int {
        return when (list[0]) {
            is Long -> VAR_LIST
            is String -> STR_LIST
            is TdfStruct -> STRUCT_LIST
            is Tripple -> TRIPPLE_LIST
            is Float -> FLOAT_LIST
            else -> throw IllegalArgumentException("List contains unknown content for first element")
        }
    }

    fun writeListElement(value: Any?, out: ByteBuf) {
        when (value) {
            is Long -> out.writeVarInt(value)
            is String -> out.writeString(value)
            is TdfStruct -> value.write(out, false)
            is Tripple -> {
                out.writeVarInt(value.a)
                out.writeVarInt(value.b)
                out.writeVarInt(value.c)
            }
            is Float -> out.writeFloat(value)
        }
    }
}


class PacketBuilder(private val component: Int, private val command: Int, private val qtype: Int, private val id: Int) : TdfGroup() {

    private var error: Int = 0

    fun Error(error: Int): PacketBuilder {
        this.error = error
        return this
    }

    fun build(): ByteBuf {
        val contentBuff = Unpooled.buffer()
        write(contentBuff, true)
        val buff = Unpooled.buffer();
        val length = contentBuff.readableBytes()
        buff.writeByte((length and 0xFFFF) shr 8)
        buff.writeByte((length and 0xFF))
        buff.writeByte(component shr 8)
        buff.writeByte(component and 0xFF)
        buff.writeByte(command shr 8)
        buff.writeByte(command and 0xFF)
        buff.writeByte(error shr 8)
        buff.writeByte(error and 0xFF)
        buff.writeByte(qtype shr 8)
        if (length > 0xFFFF) {
            buff.writeByte(0x10)
        } else {
            buff.writeByte(0x00)
        }
        buff.writeByte(id shr 8)
        buff.writeByte(id and 0xFF)

        if (length > 0xFFFF) {
            buff.writeByte(((length.toLong() and 0xFF000000) shr 24).toInt())
            buff.writeByte((length and 0x00FF0000) shr 16)
        }

        buff.writeBytes(contentBuff)

        return buff
    }
}


object PacketComponents {
    const val AUTHENTICATION = 0x1
    const val EXAMPLE = 0x3
    const val GAME_MANAGER = 0x4
    const val REDIRECTOR = 0x5
    const val PLAY_GROUPS = 0x6
    const val STATS = 0x7
    const val UTIL = 0x9
    const val CENSUS_DATA = 0xA
    const val CLUBS = 0xB
    const val GAME_REPORT_LEGACY = 0xC
    const val LEAGUE = 0xD
    const val MAIL = 0xE
    const val MESSAGING = 0xF
    const val LOCKER = 0x14
    const val ROOMS = 0x15
    const val TOURNAMENTS = 0x17
    const val COMMERCE_INFO = 0x18
    const val ASSOCIATION_LISTS = 0x19
    const val GPS_CONTENT_CONTROLLER = 0x1B
    const val GAME_REPORTING = 0x1C
    const val DYNAMIC_FILTER = 0x7D0
    const val RSP = 0x801
    const val USER_SESSIONS = 0x7802

    val Names = mapOf(
        AUTHENTICATION to "Authentication",
        EXAMPLE to "Example",
        GAME_MANAGER to "Game Manager",
        REDIRECTOR to "Redirector",
        PLAY_GROUPS to "Play Groups",
        STATS to "Stats",
        UTIL to "Util",
        CENSUS_DATA to "Census Data",
        CLUBS to "Clubs",
        GAME_REPORT_LEGACY to "Game Report Legacy",
        LEAGUE to "League",
        MAIL to "Mail",
        MESSAGING to "Messaging",
        LOCKER to "Locker",
        ROOMS to "Rooms",
        TOURNAMENTS to "Tournaments",
        COMMERCE_INFO to "Commerce Info",
        ASSOCIATION_LISTS to "Association Lists",
        GPS_CONTENT_CONTROLLER to "GPS Content Controller",
        GAME_REPORTING to "Game Reporting",
        DYNAMIC_FILTER to "Dynamic Filter",
        RSP to "RSP",
        USER_SESSIONS to "User Sessions"
    )

    fun getName(value: Int): String = Names.getOrDefault(value, "Unknown")

}

fun Packet(component: Int, command: Int, qtype: Int, id: Int, content: PacketBuilder.() -> Unit): ByteBuf {
    val context = PacketBuilder(component, command, qtype, id)
    context.content()
    return context.build()
}
