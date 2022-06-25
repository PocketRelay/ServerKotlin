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
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmOverloads
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

            val classPackage = classDeclaration.packageName
            val className = classDeclaration.simpleName.toString()
            val functionDeclarations: Sequence<KSFunctionDeclaration> =
                classDeclaration.getAllFunctions()
                    .filter { it.isAnnotationPresent(PacketHandler::class) }

            val functionMappings = HashMap<Int, String>()

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

                val command = handlerAnnotation.command
                val component = handlerAnnotation.component

                val lookupKey = ((component shl 16) + command)
                val functionName = functionDeclaration.simpleName.toString()

                functionMappings[lookupKey] = functionName
            }

            val imports = """
                |import com.jacobtread.kme.blaze.*
                |import io.netty.channel.ChannelHandlerContext
            """.trimMargin()

            val codeTextBuilder = StringBuilder()
            codeTextBuilder.append("""
                |val lookupKey: Int = ((msg.component shl 16) + msg.command
                |try {
                |   when(lookupKey) {
            """.trimMargin())
            for ((lookupKey, functionName) in functionMappings) {
                codeTextBuilder.append("    ")
                    .append(lookupKey)
                    .append(" -> ")
                    .append(functionName)
                    .append("(msg)")
            }
            codeTextBuilder.append(""""
                |    else -> {
                |       ctx.write(respond())
                |    }
            """.trimMargin())
            // Initialize the lookup variable

            val channelRead0 = FunSpec.builder("channelRead0")
                .addParameter("ctx", ChannelHandlerContext::class)
                .addParameter("msg", Packet::class)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(CodeBlock.of(codeTextBuilder.toString()))

            val classBuilder = TypeSpec.classBuilder("${className}Impl")
                .superclass(SimpleChannelInboundHandler::class.parameterizedBy(Packet::class))
                .addModifiers(KModifier.ABSTRACT)


        }

        return bad
    }
}
