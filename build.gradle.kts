plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.6.20"
    application
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

tasks.create("fatJar", Jar::class.java) {
    manifest {
        attributes["Implementation-Title"] = "KME Bundle Fat Jar"
        attributes["Main-Class"] = "com.jacobtread.kme.AppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

application {
    mainClass.set("com.jacobtread.kme.App")
    tasks.run.get().workingDir = File(projectDir, "run")
}