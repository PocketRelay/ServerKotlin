val kme3Version: String by project

group = "com.jacobtread.kme"
version = kme3Version

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("com.google.devtools.ksp") apply false
    idea
}

allprojects {
    repositories {
        mavenCentral()
    }
}


// Adding sources for generated code
idea {
    module {
        // NOTE: Don't make these into variables it will break the build step
        sourceDirs = sourceDirs + file("app/build/generated/ksp/main/kotlin")
        testSourceDirs = testSourceDirs + file("app/build/generated/ksp/test/kotlin")
        generatedSourceDirs = generatedSourceDirs + file("app/build/generated/ksp/main/kotlin") + file("app/build/generated/ksp/test/kotlin")
    }
}