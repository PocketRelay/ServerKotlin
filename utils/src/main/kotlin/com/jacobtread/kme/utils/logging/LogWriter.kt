package com.jacobtread.kme.utils.logging

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream
import kotlin.io.path.*

class LogWriter {
    private val file = Path("logs", "latest.log")
    private val fileAccess: RandomAccessFile
    private val outputBuffer: ByteBuffer = ByteBuffer.allocate(4024)

    init {
        try {
            archiveOldLog()
            fileAccess = createFileAccess()
            Runtime.getRuntime().addShutdownHook(Thread(this::close))
        } catch (e: IOException) {
            System.err.println("Failed to open RandomAccessFile to the logging file")
            throw e
        }
    }

    fun write(text: String) {
        val bytes = text.toByteArray()
        if (bytes.size > outputBuffer.remaining()) {
            synchronized(fileAccess) {
                flush()
                fileAccess.write(bytes)
            }
        } else {
            outputBuffer.put(bytes)
        }
    }

    @Synchronized
    fun flush() {
        outputBuffer.flip()
        try {
            fileAccess.channel.write(outputBuffer)
        } finally {
            outputBuffer.clear()
        }
    }

    @Synchronized
    private fun createFileAccess(): RandomAccessFile {
        val parent = file.parent
        if (parent.notExists()) parent.createDirectories()
        if (file.notExists()) file.createFile()
        val fileAccess = RandomAccessFile(file.toFile(), "rw")
        val length = fileAccess.length()
        fileAccess.seek(length)
        return fileAccess
    }


    @Synchronized
    fun archiveOldLog() {
        if (!file.isRegularFile()) return
        val logsPath = Path("logs")
        val lastModified = file.getLastModifiedTime().toInstant()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = dateFormat.format(lastModified)
        var newPath: Path
        var i = 1
        while (true) {
            newPath = logsPath.resolve("$date-$i.log.gz")
            if (newPath.notExists()) break
            i++
        }
        val inputStream = file.inputStream()
        val outputStream = GZIPOutputStream(newPath.outputStream(StandardOpenOption.CREATE).buffered())
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()
        file.deleteExisting()
    }

    @Synchronized
    fun close() {
        try {
            flush()
            fileAccess.close()
        } catch (e: Exception) {
            println("Failed to save log")
            e.printStackTrace()
        }
    }
}