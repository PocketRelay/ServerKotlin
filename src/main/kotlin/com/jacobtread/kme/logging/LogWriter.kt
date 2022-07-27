package com.jacobtread.kme.logging

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.io.path.*

/**
 * LogWriter Handles writing of logs to disk as well as keeping
 * a buffer for handling data < 4M before needing to write to disk
 *
 * @constructor Create empty LogWriter
 */
class LogWriter {

    private val fileChannel: FileChannel
    private val outputBuffer: ByteBuffer = ByteBuffer.allocate(4024)

    init {
        try {
            archiveOldLog()
            fileChannel = createFileChannel()
            // Shutdown hook for closing the file
            Runtime.getRuntime().addShutdownHook(Thread(this::close))
        } catch (e: IOException) {
            System.err.println("Failed to open RandomAccessFile to the logging file")
            throw e
        }
    }

    /**
     * write Writes text to this log writer. If the text is small
     * enough to fit into the output buffer then it is written there
     * otherwise the output buffer is flushed at the text is written
     * directly to the file channel
     *
     * @param text The text to write
     */
    fun write(text: String) {
        val bytes = text.toByteArray()
        if (bytes.size > outputBuffer.remaining()) {
            synchronized(fileChannel) {
                flush()
                fileChannel.write(ByteBuffer.wrap(bytes))
            }
        } else {
            outputBuffer.put(bytes)
        }
    }

    /**
     * flush Flushes the current contents of the output buffer
     * by writing them all to the file channel
     */
    @Synchronized
    fun flush() {
        outputBuffer.flip()
        try {
            fileChannel.write(outputBuffer)
        } finally {
            outputBuffer.clear()
        }
    }

    /**
     * createFileChannel Creates a file channel for writing to
     * the latest log file this also ensures the logging directory
     * as well as the logging file exists before opening the channel
     *
     * @return The opened file channel
     */
    @Synchronized
    private fun createFileChannel(): FileChannel {
        val parent = Path("logs")
        val file = parent.resolve("latest.log")
        if (parent.notExists()) parent.createDirectories()
        if (file.notExists()) file.createFile()
        return FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }


    /**
     * archiveOldLog Archives the old "latest.log" file by renaming it
     * to the date of last modified yyyy-MM-dd-i.log.gz (e.g 2022-04-06-1.log.gz)
     * "i" will increase until a file with that name is not taken. The
     * contents of the log are compressed using GZIP
     */
    @Synchronized
    fun archiveOldLog() {
        val file = Path("logs", "latest.log")
        if (!file.isRegularFile()) return
        val logsPath = Path("logs")
        val lastModified = file.getLastModifiedTime().toMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = dateFormat.format(Date(lastModified))
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

    /**
     * close Handles closing the log writer this flushes
     * the buffer writing everything to the log file and
     * closing the random access file
     */
    @Synchronized
    fun close() {
        try {
            flush()
            fileChannel.close()
        } catch (e: Exception) {
            println("Failed to save log")
            e.printStackTrace()
        }
    }
}