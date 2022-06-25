package com.jacobtread.kme.blaze

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.jacobtread.kme.blaze.annotations.PacketHandler
import com.jacobtread.kme.blaze.annotations.PacketProcessor
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import kotlin.reflect.KClass

class BlazeProcessor(
    val options: Map<String, String>,
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
) : SymbolProcessor {
    private fun Resolver.getWithAnnotation(clazz: KClass<*>) =
        getSymbolsWithAnnotation(clazz.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classes = resolver.getWithAnnotation(PacketProcessor::class)
        val bad = ArrayList<KSAnnotated>()

        for (classDeclaration in classes) {

            val classPackage = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val functionDeclarations: Sequence<KSFunctionDeclaration> =
                classDeclaration.getAllFunctions()
                    .filter { it.isAnnotationPresent(PacketHandler::class) }

            val functionMappings = LinkedHashMap<Int, LinkedHashMap<Int, String>>()

            val classType = classDeclaration.asType(emptyList()).toTypeName()

            for (functionDeclaration in functionDeclarations) {

                val handlerAnnotation = functionDeclaration.getAnnotationsByType(PacketHandler::class)
                    .firstOrNull() ?: continue
                val parameters = functionDeclaration.parameters
                if (parameters.isEmpty()) {
                    logger.error("Function with PacketHandler annotation had no parameters", functionDeclaration)
                    continue
                }

                if (parameters.size > 1) {
                    logger.error("Function with PacketHandler annotation had too many parameters", functionDeclaration)
                    continue
                }

                val component = handlerAnnotation.component
                val command = handlerAnnotation.command

                val functionName = functionDeclaration.simpleName.asString()

                val values = functionMappings.getOrPut(component) { LinkedHashMap() }
                values[command] = functionName
            }

            val codeTextBuilder = StringBuilder()
            codeTextBuilder.appendLine("when(msg.component) {")
            for ((component, map) in functionMappings) {
                codeTextBuilder.append("  0x")
                    .append(component.toString(16))
                    .append(" -> when (msg.command) {\n")
                for ((command, functionName) in map) {
                    codeTextBuilder.append("      0x")
                        .append(command.toString(16))
                        .append(" -> processor.")
                        .append(functionName)
                        .append("(msg)\n")
                }
                codeTextBuilder.appendLine("      else -> channel.write(msg.respond())")
                    .appendLine("  }")
            }
            codeTextBuilder.append(
                """
                |  else -> channel.write(msg.respond())
                |}
            """.trimMargin()
            )
            // Initialize the lookup variable

            val routeFunc = FunSpec.builder("route")
                .addParameter("processor", classType)
                .addParameter("channel", Channel::class)
                .addParameter("msg", Packet::class)
                .addCode(CodeBlock.of(codeTextBuilder.toString()))
                .build()

            val fileName = "${className}Router"

            val classBuilder = TypeSpec.objectBuilder(fileName)
                .addFunction(routeFunc)

            val clazz = classBuilder.build()
            val file = FileSpec.builder(classPackage, fileName)
                .addImport("io.netty.channel", "ChannelHandlerContext")
                .addImport("com.jacobtread.kme.blaze", "respond", "NotAuthenticatedException", "LoginError", "Packet")
                .addImport("com.jacobtread.kme.utils.logging", "Logger")
                .addImport("kotlin.system", "measureNanoTime")
                .addType(clazz)
                .build()
            file.writeTo(codeGenerator, false)
        }

        return bad
    }
}
