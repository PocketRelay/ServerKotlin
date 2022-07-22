group = "com.jacobtread.kme"
version = "1.0.1"

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("com.google.devtools.ksp") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
