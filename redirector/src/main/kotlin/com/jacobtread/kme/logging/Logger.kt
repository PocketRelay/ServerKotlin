package com.jacobtread.kme.logging

import java.io.BufferedOutputStream
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

class Logger {

    companion object {
        const val COLOR_REST = "\u001B[0m"
        private val ROOT = Logger()

        @JvmStatic
        fun setLogLevel(level: Level) {
            ROOT.logLevel = level
        }

        @JvmStatic
        fun get(): Logger {
            return ROOT
        }
    }


    private val printDateFormat = SimpleDateFormat("HH:mm:ss")
    private var logLevel: Level = Level.INFO

    /**
     * isDebugEnabled Checks whether the current
     * logging level is debug logging
     *
     * @return
     */
    val isDebugEnabled: Boolean get() = logLevel == Level.DEBUG

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
            if (hasArgs) {
                val throwable = args[0] as Throwable
                throwable.printStackTrace()
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
            exceptions?.forEach {
                it.printStackTrace()
            }
        }
    }
}