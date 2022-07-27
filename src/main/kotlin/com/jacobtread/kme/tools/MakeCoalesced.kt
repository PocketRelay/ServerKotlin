@file:JvmName("MakeCoalesced")

package com.jacobtread.kme.tools

import java.nio.file.Files
import java.nio.file.Paths

object MakeCoalesced {

    /**
     * main function for converting the ME3BINI into the compressed
     * and base64 version, so it doesn't need to happen at runtime
     *
     *
     * THIS SCRIPT EXPECTS TO BE EXECUTED WITH THE ROOT OF THE
     * PROJECT AS THE WORKING DIR DOING SO FROM ELSEWHERE COULD
     * BE PROBLEMATIC AND CAUSE FOLDERS IN THE WRONG PLACES
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val inFile = Paths.get("data/bini.bin")
        require(Files.exists(inFile) && Files.isRegularFile(inFile)) { "Input file data/bini.bin didn't exist or was not a file." }
        val destFile = Paths.get("app/src/main/resources/data/bini.bin.chunked")
        ResourceProcessing.processCoalesced(inFile, destFile)
        require(Files.exists(destFile) && Files.isRegularFile(destFile)) { "Something went wrong output file was never generate" }
    }
}
