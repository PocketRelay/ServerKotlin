import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kme3Version: String by project

group = "com.jacobtread.kme"
version = kme3Version

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.google.devtools.ksp")
    idea
}

repositories {
    mavenCentral()
}

dependencies {
    // Core dependency groupings

    val nettyHttpVersion: String by project
    implementation("com.jacobtread.netty:kotlin-netty-http:$nettyHttpVersion")

    val nettyVersion: String by project
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")

    val blazeVersion: String by project

    implementation("com.jacobtread.blaze:blaze-core:$blazeVersion")
    implementation("com.jacobtread.blaze:blaze-annotations:$blazeVersion")

    // KSP annoatation processing for packet routing
    ksp("com.jacobtread.blaze:blaze-processor:$blazeVersion")

    val xmlVersion: String by project
    implementation("com.jacobtread.xml:xml-builder-kt:$xmlVersion")
}


/**
 * This task generates a constants file at src/main/kotlin/com/jacobtread/kme/data/Constants.kt
 * using the template file at src/main/resources/templates/Constants.kt.template this task replaces
 * placeholders in the template file with information from the project. This is run before compile
 */
tasks.register("generateConstants") {
    val input = file("src/main/resources/templates/Constants.kt.template")
    val propertiesFile = rootDir.absoluteFile.resolve("gradle.properties")
    val output = file("src/main/kotlin/com/jacobtread/kme/data/Constants.kt")

    inputs.files(input, propertiesFile)
    outputs.file(output)

    doFirst {
        var templateFile = input.readText(Charsets.UTF_8)
        templateFile = replaceConstants(templateFile)
        output.writeText(templateFile, Charsets.UTF_8)
    }
}

/**
 * This function handles replacing the individual different
 * constant variables within the template string
 *
 * @param value The template string
 * @return The replaced template string
 */
fun replaceConstants(value: String): String {
    val kme3Version: String by project
    return value.replace("%KME_VERSION%", kme3Version)
}


/*
 * Hooks into the kotlin compiling task to set the
 * jvm target and add the defaults' compiler arg
 */
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    dependsOn("generateConstants")

    kotlinOptions {
        val javaCompileVersion: String by project
        jvmTarget = javaCompileVersion
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }
}

/*
 * Hooks into the Jar tasks to change the file name and
 * manifest contents
 */
tasks.withType(Jar::class) {
    val outputJarFile: String by project
    archiveFileName.set(outputJarFile) // Set the output jar name to server.jar
    manifest {
        // Set the main class of the jar in the manifest
        attributes["Main-Class"] = "com.jacobtread.kme.App"
    }
    exclude("templates/**")
}


/**
 * Gradle task for starting the application
 */
tasks.create("startApp", JavaExec::class) {
    mainClass.set("com.jacobtread.kme.App")
    classpath(sourceSets["main"].runtimeClasspath)
    workingDir(rootProject.projectDir.resolve("run"))
}

/**
 * Gradle task for generating the bini.bin.chunked Coalesced file
 * to use this first place the coalesced file at "data/bini.bin"
 * from the root directory then execute this task
 */
tasks.create("makeCoalesced", JavaExec::class) {
    mainClass.set("com.jacobtread.kme.tools.MakeCoalesced")
    classpath(sourceSets["main"].runtimeClasspath)
    workingDir(rootProject.projectDir)
}

/**
 * Gradle task for generated the tlk files place all the tlk files
 * in the data/tlk directory ME3TLK.tlk will be used as the default
 * file and all other languages should be named ME3TLK_${LANG_CODE}.tlk
 */
tasks.create("makeTLKs", JavaExec::class) {
    mainClass.set("com.jacobtread.kme.tools.MakeTLKs")
    classpath(sourceSets["main"].runtimeClasspath)
    workingDir(rootProject.projectDir)
}


// Adding sources for generated code
idea {
    module {
        // NOTE: Don't make these into variables it will break the build step
        sourceDirs = sourceDirs + file("build/generated/ksp/main/kotlin")
        testSourceDirs = testSourceDirs + file("build/generated/ksp/test/kotlin")
        generatedSourceDirs = generatedSourceDirs + file("build/generated/ksp/main/kotlin") + file("build/generated/ksp/test/kotlin")
    }
}

tasks.withType(ShadowJar::class) {
    minimize()
}
