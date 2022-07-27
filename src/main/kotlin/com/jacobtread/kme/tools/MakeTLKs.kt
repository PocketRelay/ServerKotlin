@file:JvmName("MakeTLKs")

package com.jacobtread.kme.tools

import kotlin.io.path.*

object MakeTLKs {
    /**
     * main Function for processing TLK files in the data/tlk directory.
     * Converts the files into base64 encoded chunks for sending over the
     * packet system
     *
     * THIS SCRIPT EXPECTS TO BE EXECUTED WITH THE ROOT OF THE
     * PROJECT AS THE WORKING DIR DOING SO FROM ELSEWHERE COULD
     * BE PROBLEMATIC AND CAUSE FOLDERS IN THE WRONG PLACES
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val tlkDir = Path("data/tlk")
        val outDir = Path("app/src/main/resources/data/tlk")
        if (!outDir.exists()) outDir.createDirectories()
        tlkDir.forEachDirectoryEntry {
            val fileName = it.fileName.toString()
            if (fileName.endsWith(".tlk")) {
                val newName = when (fileName) {
                    "ME3TLK.tlk" -> "default.tlk.chunked"
                    else -> {
                        val locale = fileName.substring(7, fileName.length - 4)
                        "$locale.tlk.chunked"
                    }
                }
                val outFile = outDir / newName
                ResourceProcessing.processTlkFile(it, outFile)
            }
        }
    }
}