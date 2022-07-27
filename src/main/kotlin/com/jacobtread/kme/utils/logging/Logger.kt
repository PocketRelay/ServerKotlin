@file:Suppress("unused")

package com.jacobtread.kme.utils.logging

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
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
    private val printDateFormat = SimpleDateFormat("HH:mm:ss")

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

    // Whether to log packets
    var logPackets = false
        private set

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
     * @param packets Whether to log packet input and output
     */
    fun init(
        levelName: String,
        file: Boolean,
    ) {
        level = Level.fromName(levelName)
        saveFile = file
    }

    fun info(text: String) = append(Level.INFO, text)
    fun info(text: String, throwable: Throwable) = appendThrowable(Level.INFO, text, throwable)
    fun info(text: String, vararg values: Any?) = appendVarargs(Level.INFO, text, values)

    fun warn(text: String) = append(Level.WARN, text)
    fun warn(text: String, throwable: Throwable) = appendThrowable(Level.WARN, text, throwable)
    fun warn(text: String, vararg values: Any?) = appendVarargs(Level.WARN, text, values)

    fun fatal(text: String): Nothing {
        append(Level.FATAL, text)
        exitProcess(1)
    }

    fun fatal(text: String, throwable: Throwable): Nothing {
        appendThrowable(Level.FATAL, text, throwable)
        exitProcess(1)
    }

    fun fatal(text: String, vararg values: Any?): Nothing {
        appendVarargs(Level.FATAL, text, values)
        exitProcess(1)
    }

    inline fun logIfDebug(text: () -> String) {
        if (debugEnabled) debug(text())
    }

    fun debug(text: String) = append(Level.DEBUG, text)
    fun debug(text: String, throwable: Throwable) = appendThrowable(Level.DEBUG, text, throwable)
    fun debug(text: String, vararg values: Any?) = appendVarargs(Level.DEBUG, text, values)

    fun error(text: String) = append(Level.ERROR, text)
    fun error(text: String, throwable: Throwable) = appendThrowable(Level.ERROR, text, throwable)
    fun error(text: String, vararg values: Any?) = appendVarargs(Level.ERROR, text, values)

    fun log(level: Level, text: String) = append(level, text)
    fun log(level: Level, text: String, throwable: Throwable) = appendThrowable(level, text, throwable)
    fun log(level: Level, text: String, vararg values: Any?) = appendVarargs(level, text, values)

    /**
     * append Appends a simple message to the log.
     *
     * @param level The level of logging for this log
     * @param message The message for this log
     */
    private fun append(level: Level, message: String) {
        if (level.index > Logger.level.index) return
        val time = printDateFormat.format(Date())
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
    private fun appendThrowable(level: Level, message: String, throwable: Throwable) {
        if (level.index > Logger.level.index) return
        append(level, message)
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

    /**
     * appendVarargs Appends a log that uses vararg arguments. This does replacements
     * of {} variables using the provided args as well as providing a stack trace for
     * all the exceptions that were provided
     *
     * @param level The log level
     * @param message The base message to log
     * @param args The provided arguments
     */
    private fun appendVarargs(level: Level, message: String, args: Array<out Any?>) {
        if (level.index > Logger.level.index) return
        val time = printDateFormat.format(Date())
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
            builder.append(arg?.toString() ?: "null")
            last = index + 2
        }
        i++
        while (i < args.size) {
            val arg = args[i]
            if (arg is Throwable) {
                if (exceptions == null) exceptions = ArrayList()
                exceptions.add(arg)
            }
            i++
        }
        if (last < message.length) {
            builder.append(message.substring(last))
        }
        val text = "[$time] ${level.coloredText()} $builder"
        val stream = if (level.index < 3) System.err else System.out
        stream.println(text)
        if (saveFile) {
            writer?.write("[$time] [${level.levelName}] $builder\n")
            val exStringWriter = StringWriter() // String writer to write the exceptions to
            val exPrintWriter = PrintWriter(exStringWriter) // Print writer abstraction
            exceptions?.forEach {
                it.printStackTrace(stream) // Print the exception to the console
                it.printStackTrace(exPrintWriter) // Print the exception to the writer
                exPrintWriter.println() // Print a new line
            }
            exPrintWriter.flush() // Flush the writer
            writer?.write(exStringWriter.toString()) // Write the string to the log file
        } else {
            exceptions?.forEach { it.printStackTrace() } // Print the exceptions to console
        }
    }
}