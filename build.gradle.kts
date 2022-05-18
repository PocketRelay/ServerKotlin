plugins {
    val kotlinVersion = "1.6.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

allprojects {


    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    repositories {
        mavenCentral()
    }

    val nettyVersion: String by project

    dependencies {
        implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
        implementation("io.netty:netty-all:$nettyVersion")
    }
}
