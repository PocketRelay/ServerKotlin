package com.jacobtread.relay.tools

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.Deflater

object ResourceProcessing {

    fun processCoalesced(file: Path, output: Path) {
        require(Files.exists(file)) { "No coalesced file at ${file.toAbsolutePath()}" }
        require(Files.isRegularFile(file)) { "Path ${file.fileName} is not a file" }
        val result = createCompressedCoalesced(Files.readAllBytes(file))
        if (Files.notExists(output)) Files.createFile(output)
        Files.writeString(output, result)
    }

    private fun createCompressedCoalesced(contents: ByteArray): String {
        val bodyBytes = compressByteArray(contents)
        val bodySize = bodyBytes.size
        val outputBuffer = ByteBuffer.allocate(16 + bodySize)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.put('N'.code.toByte())
        outputBuffer.put('I'.code.toByte())
        outputBuffer.put('B'.code.toByte())
        outputBuffer.put('C'.code.toByte())
        outputBuffer.putInt(1)
        outputBuffer.putInt(bodySize) // 8 -> 12
        outputBuffer.putInt(contents.size) // 12 -> 16
        outputBuffer.put(bodyBytes)
        outputBuffer.rewind()

        val output = ByteArray(outputBuffer.remaining())
        outputBuffer.get(output)
        return orderChunkedBase64(output)
    }

    private fun compressByteArray(contents: ByteArray): ByteArray {
        val compress = Deflater()
        compress.setLevel(6)
        compress.setInput(contents)
        compress.finish()
        val bodyStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!compress.finished()) {
            val size = compress.deflate(buffer)
            bodyStream.write(buffer, 0, size)
        }
        return bodyStream.toByteArray()
    }

    fun processTlkFile(file: Path, output: Path) {
        require(Files.exists(file)) { "No tlk file at ${file.toAbsolutePath()}" }
        require(Files.isRegularFile(file)) { "Path ${file.fileName} is not a file" }
        val result = orderChunkedBase64(Files.readAllBytes(file))
        if (Files.notExists(output)) Files.createFile(output)
        Files.writeString(output, result)
    }


    private fun orderChunkedBase64(bytes: ByteArray): String {
        val base64 = Base64.getEncoder()
            .encodeToString(bytes)

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
                .append("=")
                .append(value)
                .append('\n')
        }
        outBuilder.append("CHUNK_SIZE=255\nDATA_SIZE=")
            .append(base64.length)
        return outBuilder.toString()
    }
}
