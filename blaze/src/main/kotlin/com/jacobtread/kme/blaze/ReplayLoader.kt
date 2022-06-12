package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled
import kotlin.io.path.*

/**
 * main Simple program takes all the content from the replays' directory
 * (Packet logs etc.) and reads all the packet and places the decoded
 * variants into the decoded directory
 */
fun main() {
    val dir = Path("data/replay")
    val decodedDir = dir / "decoded"

    if (!decodedDir.exists()) decodedDir.createDirectories()
    dir.forEachDirectoryEntry {
        if (it.fileName.toString().endsWith(".bin")) {
            val outFile = decodedDir / "${it.fileName}.txt"
            val outFileRaw = decodedDir / "${it.fileName}.raw.txt"
            if (!outFile.exists()) outFile.createFile()
            val outBuilder = StringBuilder()
            val contents = it.readBytes()
            val buffer = Unpooled.wrappedBuffer(contents)
            val bufferRaw = StringBuilder()
            while (buffer.readableBytes() > 0) {
                try {
                    val length = buffer.readUnsignedShort();
                    val component = buffer.readUnsignedShort()
                    val command = buffer.readUnsignedShort()
                    val error = buffer.readUnsignedShort()
                    val qtype = buffer.readUnsignedShort()
                    val id = buffer.readUnsignedShort()
                    val extLength = if ((qtype and 0x10) != 0) buffer.readUnsignedShort() else 0
                    val contentLength = length + (extLength shl 16)
                    val content = buffer.readBytes(contentLength)
                    content.markReaderIndex()
                    val packet = Packet(component, command, error, qtype, id, content)
                    outBuilder.append(packetToBuilder(packet))
                    outBuilder.append('\n')
                    content.resetReaderIndex()
                    var count = 0
                    while (content.readableBytes() > 0) {
                        val byte = content.readUnsignedByte()
                        bufferRaw
                            .append(byte.toInt() and 255)
                            .append(", ")
                        count++
                        if (count == 12) {
                            bufferRaw.append('\n')
                            count = 0
                        }
                    }
                    bufferRaw.appendLine()
                    bufferRaw.appendLine()
                    content.release()

                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
            buffer.readerIndex(0)
            outFile.writeText(outBuilder.toString())
            outFileRaw.writeText(bufferRaw.toString())
        }
    }
}