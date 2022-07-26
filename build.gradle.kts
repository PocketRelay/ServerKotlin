val kme3Version: String by project

group = "com.jacobtread.kme"
version = kme3Version

plugins {
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("com.google.devtools.ksp") apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

