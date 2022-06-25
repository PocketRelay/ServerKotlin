package com.jacobtread.kme.blaze

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jacobtread.kme.blaze.annotations.PacketHandler
import com.jacobtread.kme.blaze.annotations.PacketProcessor
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.netty.channel.Channel
import java.util.*

class BlazeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classes = resolver.getSymbolsWithAnnotation(PacketProcessor::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
        classes.forEach { clazz ->
            // Class type and name
            val classType = clazz.asType(emptyList()).toTypeName()
            val className = clazz.simpleName.asString()
            // Using tree maps to order the components and commands
            val functionMappings = TreeMap<Int, TreeMap<Int, String>> { o1, o2 -> o1.compareTo(o2) }
            for (function in clazz.getAllFunctions()) {
                val annotation = function.getAnnotationsByType(PacketHandler::class)
                    .firstOrNull() ?: continue
                val parameters = function.parameters
                if (parameters.isEmpty()) {
                    logger.error("Function with PacketHandler annotation has an invalid number of parameters expected 1", function)
                    continue
                }
                val component = annotation.component
                val command = annotation.command
                val functionName = function.simpleName.asString()
                val values = functionMappings.getOrPut(component) { TreeMap { o1, o2 -> o1.compareTo(o2) } }
                values[command] = functionName
            }

            // String builder for building the code string
            val codeBuilder = StringBuilder("when(msg.component) {\n") // Build starts with opening component when statement
            // Iterate over the function mappings outer components
            functionMappings.forEach { (component, fmap) ->
                codeBuilder.append("  ") // Ident for when statement
                    .append(component)
                    .appendLine(" -> when (msg.command) {") // Start next when statement for commands

                fmap.forEach { (command, functionName) ->
                    // Append function call for the command
                    codeBuilder.append("    ")
                        .append(command)
                        .append(" -> processor.")
                        .append(functionName)
                        .appendLine("(msg)")
                }

                // Append the fallback empty send for other commands
                codeBuilder.appendLine("    else -> channel.write(msg.respond())")
                // Close when statement
                codeBuilder.appendLine("  }")
            }
            // Append the fallback empty send for other components
            codeBuilder.appendLine("  else -> channel.write(msg.respond())")
            // Close when statement
            codeBuilder.appendLine("}")


            /**
             * routeFunc Creates a function that is used for routing this
             * is named route${TheProcesssorClassName} and takes the processor,
             * channel, and packet as its arguments
             */
            val routeFunc = FunSpec.builder("route${className}")
                .addParameter("processor", classType)
                .addParameter("channel", Channel::class)
                .addParameter("msg", Packet::class)
                .addCode(CodeBlock.of(codeBuilder.toString()))
                .build()

            // File specification
            val file = FileSpec
                .builder(clazz.packageName.asString(), "${className}RouterFunction")
                // Import for respond extension function
                .addImport("com.jacobtread.kme.blaze", "respond")
                .addFunction(routeFunc)
                .build()
            // Write the created file
            file.writeTo(codeGenerator, false)
        }
        return emptyList()
    }
}
