plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.6.20"
}

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

allprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    repositories {
        mavenCentral()
    }
}


dependencies {
    implementation(project(":core"))
}

tasks.create("start", JavaExec::class.java) {
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf()
    mainClass.set("com.jacobtread.kme.App")
    workingDir = File(projectDir, "run")
}
