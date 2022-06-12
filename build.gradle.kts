group = "com.jacobtread.kme"
version = "1.0.0"

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("com.github.johnrengelman.shadow") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
