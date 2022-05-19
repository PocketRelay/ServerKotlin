import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    val kotlinVersion = "1.6.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

allprojects {


    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
    }

    val nettyVersion: String by project

    dependencies {
        implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
        implementation("io.netty:netty-all:$nettyVersion")
    }
}
