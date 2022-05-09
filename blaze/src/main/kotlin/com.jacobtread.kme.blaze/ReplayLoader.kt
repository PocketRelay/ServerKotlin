package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled
import java.io.FileNotFoundException
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.readBytes

fun main(args: Array<String>) {

    val dir = Paths.get("replay")
    dir.forEachDirectoryEntry {
        if (it.fileName.toString().endsWith(".bin")) {
            val contents = it.readBytes()
            val buffer = Unpooled.wrappedBuffer(contents)
            while (buffer.readableBytes() > 0) {
                try {
                    println("=========== CONTENTS OF ${it.fileName.toString()} ===========")
                    val packet = RawPacket.read(buffer)
                    println(PacketDumper.dump(packet))
                    println("=======================================")
                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }


}