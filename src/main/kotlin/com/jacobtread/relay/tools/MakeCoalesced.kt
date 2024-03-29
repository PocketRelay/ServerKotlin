@file:JvmName("MakeCoalesced")

package com.jacobtread.relay.tools

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

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
        val time = measureTimeMillis {
            val inFile = Paths.get("data/coalesced.bin")
            val inputExists = Files.exists(inFile) && Files.isRegularFile(inFile)
            require(inputExists) { "Input file data/coalesced.bin didn't exist or was not a file." }
            val destFile = Paths.get("src/main/resources/data/coalesced.dmap")
            ResourceProcessing.processCoalesced(inFile, destFile)
            val outputExists = Files.exists(destFile) && Files.isRegularFile(destFile)
            require(outputExists) { "Something went wrong output file was never generate" }
        }
        println("Took ${time}ms")
    }
}
