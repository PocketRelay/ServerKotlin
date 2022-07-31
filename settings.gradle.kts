pluginManagement {
    val kspVersion: String by settings
    val kotlinVersion: String by settings
    val shadowVersion: String by settings
    val detektVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
        id("com.google.devtools.ksp") version kspVersion
        id("io.gitlab.arturbosch.detekt") version detektVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kme"
