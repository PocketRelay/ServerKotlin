@file:JvmName("MakeCoalesced")

package com.jacobtread.kme.tools

import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

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
        val inFile = Path("data/bini.bin")
        require(inFile.exists() && inFile.isRegularFile()) { "Input file data/bini.bin didn't exist or was not a file." }
        val destFile = Path("app/src/main/resources/data/bini.bin.chunked")
        ResourceProcessing.processCoalesced(inFile, destFile)
        require(destFile.exists() && destFile.isRegularFile()) { "Something went wrong output file was never generate" }
    }
}
