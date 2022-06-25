@file:JvmName("BlazeProcessorProvider")
package com.jacobtread.kme.blaze

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class BlazeProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BlazeProcessor(
            environment.options,
            environment.codeGenerator,
            environment.logger
        )
    }
}