package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled
import kotlin.io.path.*

fun main(args: Array<String>) {
    val dir = Path("replay")
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
                    val packet = Packet.read(buffer)
                    outBuilder.append(PacketDumper.dump(packet))
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