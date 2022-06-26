@file:JvmName("MakeCoalesced")

package com.jacobtread.kme.tools

import kotlin.io.path.Path

object MakeCoalesced {

    /**
     * main function for converting the ME3BINI into the compressed
     * and base64 version, so it doesn't need to happen at runtime
     *
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val inFile = Path("data/bini.bin")
        val outFile = Path("data/bini.bin.chunked")
        ResourceProcessing.processCoalesced(inFile, outFile)
    }
}
