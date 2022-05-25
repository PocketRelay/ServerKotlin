package com.jacobtread.kme.utils.tools

import io.netty.buffer.Unpooled
import java.nio.file.Path
import java.util.*
import java.util.zip.Deflater
import kotlin.io.path.*

object ResourceProcessing {

    fun processCoalesced(file: Path, output: Path) {
        require(file.exists()) { "No coalesced file at ${file.absolute()}" }
        require(file.isRegularFile()) { "Path ${file.fileName} is not a file" }
        val result = processCoalescedBytes(file.readBytes())
        if (!output.exists()) output.createFile()
        output.writeText(result)
    }

    fun processCoalescedBytes(contents: ByteArray): String {
        val compress = Deflater()
        compress.setLevel(6)
        compress.setInput(contents)
        compress.finish()
        val output = Unpooled.buffer(256)

        output.writeByte('N'.code)
        output.writeByte('I'.code)
        output.writeByte('B'.code)
        output.writeByte('C'.code)
        output.writeIntLE(1)
        output.writeIntLE(0) // 8 -> 12
        output.writeIntLE(contents.size) // 12 -> 16

        val buffer = ByteArray(1024)
        var totalSize = 0
        while (!compress.finished()) {
            val size = compress.deflate(buffer)
            output.writeBytes(buffer, 0, size)
            totalSize += size
        }

        val writeIndex = output.writerIndex()
        output.writerIndex(8)
        output.writeIntLE(totalSize)
        output.writerIndex(writeIndex)
        val bytes = ByteArray(output.readableBytes())
        output.readBytes(bytes)
        return orderChunkedBase64(bytes)
    }

    fun processTlkFile(file: Path, output: Path) {
        require(file.exists()) { "No coalesced file at ${file.absolute()}" }
        require(file.isRegularFile()) { "Path ${file.fileName} is not a file" }
        val result = processTlkBytes(file.readBytes())
        if (!output.exists()) output.createFile()
        output.writeText(result)
    }


    fun processTlkBytes(contents: ByteArray): String {
        return orderChunkedBase64(contents)
    }

    private fun orderChunkedBase64(bytes: ByteArray): String {
        val base64 = Base64.getEncoder().encodeToString(bytes)
        val chunks = base64.chunked(255)

        val keys = ArrayList<String>(chunks.size)
        val values = ArrayList<String>(chunks.size)

        chunks.forEachIndexed { index, value ->
            keys.add("CHUNK_$index")
            values.add(value)
        }

        var run = true
        while (run) {
            run = false
            var tmp: String
            for (i in 0 until keys.size - 1) {
                if (keys[i] > keys[i + 1]) {
                    tmp = keys[i]
                    keys[i] = keys[i + 1]
                    keys[i + 1] = tmp
                    tmp = values[i]
                    values[i] = values[i + 1]
                    values[i + 1] = tmp
                    run = true
                }
            }
        }

        val outBuilder = StringBuilder()
        for (i in 0 until keys.size) {
            val key = keys[i]
            val value = values[i]
            outBuilder
                .append(key)
                .append(":")
                .append(value)
                .append('\n')
        }
        outBuilder.append("CHUNK_SIZE:255\nDATA_SIZE:")
            .append(base64.length)
        return outBuilder.toString()
    }

}