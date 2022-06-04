package com.jacobtread.kme.utils.logging

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
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

    private val printDateFormat = SimpleDateFormat("HH:mm:ss")
    private var writer: LogWriter? = null
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
    var isDebugEnabled: Boolean = false
        private set

    fun init(config: Config) {
        logLevel = config.level
        isDebugEnabled = logLevel == Level.DEBUG
        logToFile = config.save
        isLogPackets = config.packets
        if (logToFile) {
            writer = LogWriter()
        }
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
        if (isDebugEnabled) debug(text())
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
        if (level.index > logLevel.index) return
        val time = printDateFormat.format(Date())
        val text = "[$time] ${level.coloredText()} $message\n"
        val stream = if (level.index < 3) System.err else System.out
        stream.print(text)
        if (logToFile) {
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
        if (level.index > logLevel.index) return
        append(level, message)
        throwable.printStackTrace()
        if (logToFile) {
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
        if (level.index > logLevel.index) return
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
        if (last < message.length) {
            builder.append(message.substring(last))
        }
        val text = "[$time] ${level.coloredText()} $builder\n"
        val stream = if (level.index < 3) System.err else System.out
        stream.print(text)
        if (logToFile) {
            writer?.write("[$time] [${level.levelName}] $builder\n")
            val exSW = StringWriter()
            val exPW = PrintWriter(exSW)
            exceptions?.forEach {
                it.printStackTrace()
                it.printStackTrace(exPW)
                exPW.println()
            }
            exPW.flush()
            writer?.write(exSW.toString())
        } else {
            exceptions?.forEach { it.printStackTrace() }
        }
    }

}