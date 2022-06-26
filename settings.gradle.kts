pluginManagement {
    val kspVersion: String by settings
    val kotlinVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("com.google.devtools.ksp") version kspVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kme"
include("app", "blaze", "utils", "blaze-processor")
