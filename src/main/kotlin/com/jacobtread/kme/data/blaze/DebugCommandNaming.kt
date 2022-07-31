package com.jacobtread.kme.data.blaze

import com.jacobtread.blaze.debug.DebugNaming
import com.jacobtread.kme.utils.logging.Logger

class DebugCommandNaming : DebugNaming {

    private fun loadNamingFile(fileName: String): Map<Int, String> {
        val fileStream = DebugCommandNaming::class.java.getResourceAsStream("/data/$fileName")
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

    override fun getComponentNames(): Map<Int, String> = loadNamingFile("components.naming")
    override fun getCommandNames(): Map<Int, String> = loadNamingFile("commands.naming")
    override fun getNotifyNames(): Map<Int, String> = loadNamingFile("notify.naming")
}
