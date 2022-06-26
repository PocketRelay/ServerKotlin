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
    databaseDrivers()
    exposedDatabaseDependencies()
    localDependencies()

    // NO-OP dependency to disable SLF4J logging that is used by the exposed library
    implementation("org.slf4j:slf4j-nop:1.7.36")

    // Subprojects for blaze networking and utilities
    implementation(project(":blaze"))
    implementation(project(":utils"))

    // KSP annoatation processing for packet routing
    ksp(project(":blaze-processor"))
}

// Adding sources for generated code
idea {
    module {
        val mainSources = file("build/generated/ksp/main/kotlin")
        val testSources = file("build/generated/ksp/test/kotlin")

        sourceDirs = sourceDirs + mainSources
        testSourceDirs = testSourceDirs + testSources
        generatedSourceDirs = generatedSourceDirs + mainSources + testSources
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
    val yamlktVersion: String by project
    val kotlinxSerializationJson: String by project
    implementation("net.mamoe.yamlkt:yamlkt:$yamlktVersion") // YAML
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
 * databaseDrivers Adds the database drivers used by this project
 * currently this is only the MySQL and SQLite drivers
 */
fun DependencyHandlerScope.databaseDrivers() {
    val mysqlDriverVersion: String by project
    val sqliteDriverVersion: String by project
    implementation("mysql:mysql-connector-java:$mysqlDriverVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteDriverVersion")
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
tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }
}


/*
 * Hooks into the Jar tasks to change the file name and
 * manifest contents
 */
tasks.withType(Jar::class.java) {
    archiveFileName.set("server.jar") // Set the output jar name to server.jar
    manifest {
        // Set the main class of the jar in the manifest
        attributes["Main-Class"] = "com.jacobtread.kme.App"
    }
}
