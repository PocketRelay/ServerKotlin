package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled
import java.io.FileNotFoundException
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readBytes

fun main(args: Array<String>) {
    val inputFile = Paths.get(args[0])
    if (!inputFile.exists()) throw FileNotFoundException("Input file not found")
    val contents = inputFile.readBytes()
    val buffer = Unpooled.wrappedBuffer(contents)
    while (buffer.readableBytes() > 0) {
        try {
            val packet = RawPacket.read(buffer)
            println(PacketDumper.dump(packet))
            println("=======================================")
        } catch (e: Throwable) {
            e.printStackTrace()
            break
        }
    }
}