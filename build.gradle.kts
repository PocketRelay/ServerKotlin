import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.6.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.jacobtread.kme"
version = "1.0.0"

allprojects {


    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
    }

    val nettyVersion: String by project

    dependencies {
        implementation("io.netty:netty-handler:$nettyVersion")
        implementation("io.netty:netty-buffer:$nettyVersion")
        implementation("io.netty:netty-codec:$nettyVersion")
        implementation("io.netty:netty-codec-http:$nettyVersion")

        // YAML Serialization for config files
        implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    }
}


val exposedVersion: String by project

@Suppress("SpellCheckingInspection")
dependencies {
    // JSON Serialization for the web
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // Local disk dependencies
    implementation(fileTree("libs") { include("*.jar") })

    // JDBC Connectors
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    // NOP to disable SLF4J
    implementation("org.slf4j:slf4j-nop:1.7.36")
    implementation(project(":blaze"))
    implementation(project(":utils"))
}

tasks.withType(ShadowJar::class.java) {
    minimize()
}

tasks.withType(Jar::class.java) {
    archiveFileName.set("server.jar")
    manifest {
        attributes["Main-Class"] = "com.jacobtread.kme.App"
    }
}

tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "17"
    }
}