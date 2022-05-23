package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.io.path.*

fun main(args: Array<String>) {
    val dir = Path("data/replay")
    val decodedDir = dir / "decoded"

    if (!decodedDir.exists()) decodedDir.createDirectories()
    dir.forEachDirectoryEntry {
        if (it.fileName.toString().endsWith(".bin")) {
            val outFile = decodedDir / "${it.fileName}.txt"
            if (!outFile.exists()) outFile.createFile()
            val outBuilder = StringBuilder()
            val contents = it.readBytes()
            val buffer = Unpooled.wrappedBuffer(contents)
            while (buffer.readableBytes() > 0) {
                try {
                    val packet = readComplete(buffer)
                    outBuilder.append(packetToBuilder(packet))
                    outBuilder.append('\n')
                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
            outFile.writeText(outBuilder.toString())
        }
    }
}

fun readComplete(input: ByteBuf): Packet {
    val length = input.readUnsignedShort();
    val component = input.readUnsignedShort()
    val command = input.readUnsignedShort()
    val error = input.readUnsignedShort()
    val qtype = input.readUnsignedShort()
    val id = input.readUnsignedShort()
    val extLength = if ((qtype and 0x10) != 0) input.readUnsignedShort() else 0
    val contentLength = length + (extLength shl 16)
    val content = input.readBytes(contentLength)
    return Packet(component, command, error, qtype, id, content)
}