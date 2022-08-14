package com.jacobtread.kme.data.blaze

import com.jacobtread.blaze.logging.BlazeLoggingHandler
import com.jacobtread.kme.utils.logging.Logger

class DebugLoggingHandler : BlazeLoggingHandler {

    override fun debug(text: String) {
        Logger.debug(text)
    }

    override fun warn(text: String) {
        Logger.warn(text)
    }

    override fun warn(text: String, cause: Throwable) {
        Logger.warn(text, cause)
    }

    override fun error(text: String) {
        Logger.error(text)
    }

    override fun error(text: String, cause: Throwable) {
        Logger.error(text, cause)
    }

    private fun loadNamingFile(fileName: String): Map<Int, String> {
        val fileStream = DebugLoggingHandler::class.java.getResourceAsStream("/data/$fileName.dmap")
            ?: return emptyMap()
        val output = HashMap<Int, String>()
        val reader = fileStream.bufferedReader()
        reader.use {
            var line: String?
            while (true) {
                line = it.readLine()
                if (line.isNullOrEmpty()) break
                val parts = line.split('=', limit = 2)
                if (parts.size < 2) {
                    Logger.warn("Naming file invalid entry: $line")
                    continue
                }
                val id = parts[0]
                    .substring(2)
                    .toIntOrNull(16)
                if (id == null) {
                    Logger.warn("Invalid ID in naming file: ${parts[0]}")
                } else {
                    val name = parts[1]
                    output[id] = name
                }
            }
        }

        if (Logger.debugEnabled) {
            Logger.debug("Loaded ${output.size} entries from \"$fileName\"")
        }

        return output
    }

    override fun getComponentNames(): Map<Int, String> = loadNamingFile("components")
    override fun getCommandNames(): Map<Int, String> = loadNamingFile("commands")
    override fun getNotifyNames(): Map<Int, String> = loadNamingFile("notify")
}
