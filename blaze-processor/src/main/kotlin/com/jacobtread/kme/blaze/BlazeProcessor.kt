package com.jacobtread.kme.blaze

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.jacobtread.kme.blaze.annotations.PacketHandler
import com.jacobtread.kme.blaze.annotations.PacketProcessor
import com.jacobtread.kme.utils.logging.Logger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmOverloads
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
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

            if (classDeclaration.classKind != ClassKind.INTERFACE) {
                bad.add(classDeclaration)
                logger.error("PacketProcessor annotation was present on non interface", classDeclaration)
                continue
            }

            val classPackage = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val functionDeclarations: Sequence<KSFunctionDeclaration> =
                classDeclaration.getAllFunctions()
                    .filter { it.isAnnotationPresent(PacketHandler::class) }

            val functionMappings = LinkedHashMap<Int, LinkedHashMap<Int, String>>()

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
            codeTextBuilder.appendLine("""
                |val timeTaken = measureTimeMillis {
                |try {
                |   when(msg.component) {
            """.trimMargin())
            for ((component, map) in functionMappings) {
                codeTextBuilder.append("    0x")
                    .append(component.toString(16))
                    .append(" -> when (msg.command) {\n")
                for ((command, functionName) in map) {
                    codeTextBuilder.append("      0x")
                        .append(command.toString(16))
                        .append(" -> ")
                        .append(functionName)
                        .append("(msg)\n")
                }
                codeTextBuilder.appendLine("""
                |      else -> ctx.write(msg.respond())
                |    }
                """.trimMargin())
            }
            val va= "\${className}"
            codeTextBuilder.append("""
                |    else -> ctx.write(msg.respond())
                |  }
                |} catch (e: NotAuthenticatedException) { // Handle player access with no player
                |  ctx.write(LoginError.INVALID_ACCOUNT(msg))
                |  val address = ctx.channel().remoteAddress()
                |  Logger.warn("Client at {} tried to access a authenticated route without authenticating", address)
                |} catch (e: Exception) {
                |  Logger.warn("Failed to handle packet: {}", msg, e)
                |  ctx.write(msg.respond())
                |}
                |}
                |ctx.flush()
                |msg.release() // Release content from message at end of handling
                |
                |Logger.debug("Request took {} ns",timeTaken)
            """.trimMargin())
            // Initialize the lookup variable

            val channelRead0 = FunSpec.builder("channelRead0")
                .addParameter("ctx", ChannelHandlerContext::class)
                .addParameter("msg", Packet::class)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(CodeBlock.of(codeTextBuilder.toString()))
                .build()

            val fileName = "${className}Impl"

            val classBuilder = TypeSpec.classBuilder(fileName)
                .superclass(SimpleChannelInboundHandler::class.parameterizedBy(Packet::class))
                .addSuperinterface( classDeclaration.asType(emptyList()).toTypeName())
                .addModifiers(KModifier.ABSTRACT)
                .addFunction(channelRead0)

            val clazz = classBuilder.build()
            val file = FileSpec.builder(classPackage, fileName)
                .addImport("io.netty.channel", "ChannelHandlerContext")
                .addImport("com.jacobtread.kme.blaze", "respond","NotAuthenticatedException", "LoginError", "Packet")
                .addImport("com.jacobtread.kme.utils.logging", "Logger")
                .addImport("kotlin.system","measureNanoTime")
                .addType(clazz)
                .build()
            file.writeTo(codeGenerator, false)
        }

        return bad
    }
}
