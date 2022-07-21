import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("com.google.devtools.ksp")
    idea
}

dependencies {

    // Core dependency groupings
    serializationDependencies()
    nettyDependencies()
    exposedDatabaseDependencies()
    localDependencies()

    // NO-OP dependency to disable SLF4J logging that is used by the exposed library
    implementation("org.slf4j:slf4j-nop:1.7.36")

    // Subprojects for blaze networking and utilities
    implementation(project(":blaze"))
    implementation(project(":logger"))

    // KSP annoatation processing for packet routing
    ksp(project(":blaze-processor"))
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


/**
 * localDependencies Adds the local dependencies stores in the ../libs
 * directory this will include any files ending in .jar
 */
fun DependencyHandlerScope.localDependencies() {
    implementation(fileTree("../libs") { include("*.jar") })
}

/**
 * serializationDependencies Adds the implementations for the
 * dependencies that this project uses for serialization
 */
fun DependencyHandlerScope.serializationDependencies() {
    val kotlinxSerializationJson: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson") // JSON
}

/**
 * nettyDependencies Adds the individual netty networking
 * components used by this project
 */
fun DependencyHandlerScope.nettyDependencies() {
    val nettyVersion: String by project
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
}

/**
 * exposedDatabaseDependencies Adds the dependencies for the
 * exposed database module
 */
fun DependencyHandlerScope.exposedDatabaseDependencies() {
    val exposedVersion: String by project
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

/*
 * Hooks into the kotlin compiling task to set the
 * jvm target and add the defaults' compiler arg
 */
tasks.withType(KotlinCompile::class) {
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
