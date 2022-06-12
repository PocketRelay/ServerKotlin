package com.jacobtread.kme.utils.tools

import kotlin.io.path.*

/**
 * main Function for processing TLK files in the data/tlk directory.
 * Converts the files into base64 encoded chunks for sending over the
 * packet system
 */
fun main() {
    val tlkDir = Path("data/tlk")
    val outDir = tlkDir / "processed"
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