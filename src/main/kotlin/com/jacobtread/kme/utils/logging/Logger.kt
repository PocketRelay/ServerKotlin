package com.jacobtread.kme.utils.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

/**
 * Logger is my personal no-fluff logging implementation It's
 * intended to be a bare not very featured logger that does its
 * job and that's it.
 *
 * @constructor Create empty Logger
 */
object Logger {

    // The date format used when printing
    private val printDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    // The log file writer null unless file logging is enabled
    private var writer: LogWriter? = null

    // The level of logging
    private var level: Level = Level.INFO
        set(value) {
            field = value
            debugEnabled = value == Level.DEBUG
        }

    // Whether to save the logs to files
    private var saveFile = false
        set(value) {
            field = value
            writer = if (value) LogWriter() else null
        }

    /**
     * isDebugEnabled Checks whether the current
     * logging level is debug logging
     *
     * @return
     */
    var debugEnabled: Boolean = false
        private set

    /**
     * init Initialize the logger settings using the
     * provided values
     *
     * @param levelName The logging level name
     * @param file Whether to save the log output to the log files
     */
    fun init(
        levelName: String,
        file: Boolean,
    ) {
        level = Level.fromName(levelName)
        saveFile = file
    }

    fun info(text: String) = append(Level.INFO, text)
    fun info(text: String, throwable: Throwable?) = appendThrowable(Level.INFO, text, throwable)

    fun warn(text: String) = append(Level.WARN, text)
    fun warn(text: String, throwable: Throwable?) = appendThrowable(Level.WARN, text, throwable)

    fun fatal(text: String): Nothing {
        append(Level.FATAL, text)
        exitProcess(1)
    }

    fun fatal(text: String, throwable: Throwable?): Nothing {
        appendThrowable(Level.FATAL, text, throwable)
        exitProcess(1)
    }


    inline fun logIfDebug(text: () -> String) {
        if (debugEnabled) debug(text())
    }

    fun debug(text: String) = append(Level.DEBUG, text)

    fun error(text: String) = append(Level.ERROR, text)
    fun error(text: String, throwable: Throwable?) = appendThrowable(Level.ERROR, text, throwable)

    fun commandResult(text: String) {
        println(text)
        if (saveFile) writer?.write(text)
    }

    /**
     * append Appends a simple message to the log.
     *
     * @param level The level of logging for this log
     * @param message The message for this log
     */
    private fun append(level: Level, message: String) {
        if (level.index > Logger.level.index) return
        val date =LocalDateTime.now()
        val time = printDateFormat.format(date)
        val text = "[$time] ${level.coloredText()} $message\n"
        val stream = if (level.index < 3) System.err else System.out
        stream.print(text)
        if (saveFile) {
            writer?.write("[$time] [${level.levelName}] $message\n")
        }
    }

    /**
     * appendThrowable Appends a log that has a thrown exception
     * attached to it. This exception will be printed and written
     * to the log file if file logging is enabled
     *
     * @param level The level of logging for this log
     * @param message The base message to log
     * @param throwable The throwable exception
     */
    private fun appendThrowable(level: Level, message: String, throwable: Throwable?) {
        if (level.index > Logger.level.index) return
        append(level, message)
        if (throwable != null) {
            throwable.printStackTrace()
            if (saveFile) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                pw.println()
                pw.flush()
                writer?.write(sw.toString())
            }
        }
    }
}
