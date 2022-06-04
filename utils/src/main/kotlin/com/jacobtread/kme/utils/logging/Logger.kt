package com.jacobtread.kme.utils.logging

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.io.path.*
import kotlin.system.exitProcess

object Logger {

    @Serializable
    data class Config(
        @Comment("The level of logging that should be used: INFO, WARN, ERROR, FATAL, DEBUG")
        val level: Level = Level.INFO,
        @Comment("Enable to keep track of logs in the logs/ directory will archive old logs with gzip")
        val save: Boolean = true,
        @Comment("Whether to log the contents of incoming and outgoing packets (For debugging)")
        val packets: Boolean = false,
    )

    private const val COLOR_REST = "\u001B[0m"
    private const val DEFAULT_BUFFER_SIZE = 4024


    private val printDateFormat = SimpleDateFormat("HH:mm:ss")
    private val loggingPath: Path = Paths.get("logs")
    private val logFile: Path = loggingPath.resolve("latest.log")
    private var file: RandomAccessFile? = null
    private var outputBuffer: ByteBuffer? = null
    private var logLevel: Level = Level.INFO
    private var logToFile = false
    var isLogPackets = false
        private set

    /**
     * isDebugEnabled Checks whether the current
     * logging level is debug logging
     *
     * @return
     */
    val isDebugEnabled: Boolean get() = logLevel == Level.DEBUG

    fun init(config: Config) {
        logLevel = config.level
        logToFile = config.save
        isLogPackets = config.packets
        if (logToFile) {
            try {
                archiveOld()
                outputBuffer =  ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
                file = createFile()
                Runtime.getRuntime().addShutdownHook(Thread(this::close))
            } catch (e: IOException) {
                System.err.println("Failed to open RandomAccessFile to the logging path")
                throw e
            }
        }
    }

    fun info(text: String, vararg args: Any? = emptyArray()) = append(Level.INFO, text, *args)

    fun warn(text: String, vararg args: Any? = emptyArray()) = append(Level.WARN, text, *args)

    fun fatal(text: String, vararg args: Any? = emptyArray()): Nothing {
        append(Level.FATAL, text, *args)
        exitProcess(1)
    }

    inline fun logIfDebug(provider: () -> String) {
        if (isDebugEnabled) {
            debug(provider())
        }
    }

    fun debug(text: String, vararg args: Any? = emptyArray()) = append(Level.DEBUG, text, *args)

    fun error(text: String, vararg args: Any? = emptyArray()) = append(Level.ERROR, text, *args)

    fun log(level: Level, text: String, vararg args: Any? = emptyArray()) = append(level, text, *args)

    /**
     * close Called when the application is closing. Flushes
     * the contents of the buffer and closes the [file]
     */
    @Synchronized
    private fun close() {
        if (!logToFile) return
        try {
            flush()
            file!!.close()
        } catch (e: Exception) {
            println("Failed to save log")
            e.printStackTrace()
        }
    }

    /**
     * flush Flushes the contents of the [outputBuffer] to
     * the file and clears the buffer
     */
    @Synchronized
    private fun flush() {
        if (!logToFile) return
        outputBuffer!!.flip()
        try {
            file!!.channel.write(outputBuffer)
        } finally {
            outputBuffer!!.clear()
        }
    }

    /**
     * append Appends a new log entry
     *
     * @param level The level of the log
     * @param message The message of the log
     * @param args Arguments to be placed into it
     */
    private fun append(level: Level, message: String, vararg args: Any?) {
        if (level.index > logLevel.index) return
        val time = printDateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val hasArgs = args.isNotEmpty()
        if (!hasArgs || args[0] is Throwable) {
            val text = "[$time] [${level.levelName}] $message\n"
            if (level.index < 3) {
                System.err.print(text)
            } else {
                print(text)
            }
            write(text)
            if (hasArgs) {
                val throwable = args[0] as Throwable
                throwable.printStackTrace()
                write(throwable.stackTraceToString() + '\n')
            }
        } else {
            var exceptions: ArrayList<Throwable>? = null
            val builder = StringBuilder()
            var i = 0
            var last = 0
            while (true) {
                val index = message.indexOf("{}", last)
                if (index < 0) break
                check(i < args.size) { "Incorrect number of arguments provided" }
                builder.append(message.substring(last, index))
                var arg = args[i]
                while (arg is Throwable) {
                    i++
                    if (exceptions == null) {
                        exceptions = ArrayList()
                    }
                    exceptions.add(arg)
                    if (i < args.size) {
                        arg = args[i]
                    } else {
                        arg = null
                        break
                    }
                }
                if (arg == null) {
                    builder.append("null")
                } else {
                    builder.append(arg.toString())
                }
                last = index + 2
            }
            if (last < message.length) {
                builder.append(message.substring(last))
            }
            val text = "[$time] [$threadName/${level.levelName}] $builder\n"
            if (level.index < 3) {
                System.err.print("${level.colorCode}$text$COLOR_REST")
            } else {
                print("${level.colorCode}$text$COLOR_REST")
            }
            write(text)
            exceptions?.forEach {
                it.printStackTrace()
                write(it.stackTraceToString() + '\n')
            }
        }
    }

    /**
     * write Writes the provided text to the [outputBuffer]
     * but if it cannot fit the output buffer will be flushed
     * to the [file] along with the bytes
     *
     * @param text The text to write to the log
     */
    private fun write(text: String) {
        if (!logToFile) return
        val bytes = text.toByteArray()
        if (bytes.size > outputBuffer!!.remaining()) {
            synchronized(file!!) {
                flush()
                file!!.write(bytes)
            }
        } else {
            outputBuffer!!.put(bytes)
        }
    }


    /**
     * createFile Creates a new logging file and
     * opens a random access file to that path
     */
    @Synchronized
    private fun createFile(): RandomAccessFile {
        val parent = logFile.parent
        if (!parent.exists()) parent.createDirectories()
        if (!logFile.exists()) logFile.createFile()
        val randomAccess = RandomAccessFile(logFile.toFile(), "rw")
        val length = randomAccess.length()
        randomAccess.seek(length)
        return randomAccess
    }

    /**
     * archiveOld Archives any existing latest.log files as
     * logs/{yyyy-MM-dd}-{i}.log.gz this is stored using the
     * gzip file format.
     */
    @Synchronized
    private fun archiveOld() {
        if (logFile.isRegularFile()) {
            val lastModified = logFile.getLastModifiedTime().toMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val date = dateFormat.format(Date(lastModified))
            var file: Path? = null
            var i = 1
            while (file == null) {
                val path = loggingPath.resolve("$date-$i.log.gz")
                if (path.exists()) {
                    i++
                    continue
                }
                file = path
            }

            val inputStream = logFile.inputStream()
            val outputStream = GZIPOutputStream(file.outputStream(StandardOpenOption.CREATE).buffered())
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            logFile.deleteExisting()
        }
    }

}