package com.jacobtread.kme.utils.tools

import io.netty.buffer.Unpooled
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.zip.Deflater
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

object ResourceProcessing {

    fun processCoalesced(file: Path) {
        require(file.exists()) { "No coalesced file at ${file.absolute()}" }
        require(file.isRegularFile()) { "Path ${file.fileName} is not a file" }
        processCoalescedBytes(file.readBytes())
    }

    fun processCoalescedBytes(contents: ByteArray) {
        val compress = Deflater()
        compress.setLevel(6)
        compress.setInput(contents)
        compress.finish()
        val output = Unpooled.buffer(256)
        output.writerIndex(16)
        val outputStream = ByteArrayOutputStream(contents.size)
        outputStream.write
        outputStream.use {
            val buffer = ByteArray(1024)
            while (!compress.finished()) {
                val size = compress.deflate(buffer)
                it.write(buffer, 0, size)
            }
        }
        val compressedBytes = outputStream.toByteArray()
        val
    }

}