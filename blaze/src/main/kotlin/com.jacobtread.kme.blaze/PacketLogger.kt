package com.jacobtread.kme.blaze

import java.util.*

object PacketLogger {

    val packets = LinkedList<Entry>()

    enum class Direction { IN, OUT }
    data class Entry(val dir: Direction, val value: RawPacket, val raw: ByteArray?)

    fun log(direction: Direction, packet: RawPacket, raw: ByteArray? = null) {
        synchronized(packets) {
            packets.add(Entry(direction, packet, raw))
        }
    }

    fun dump() {
        synchronized(packets) {
            packets.forEach {
                val (dir, packet, raw) = it
                println("DIRECTION: $dir")
                println("DECODED:")
                println(PacketDumper.dump(packet))
                if (raw != null) {
                    println("RAW:")
                    val length = raw.size
                    for (i in raw.indices) {
                        print(raw[i].toInt().and(0xFF))
                        if (i != length - 1) {
                            print(", ")
                        }
                    }
                    println()
                }
            }
        }
    }

}