plugins {
    kotlin("jvm")
}

// Variables from gradle.properties
val kotlinPoetVersion: String by project
val kspVersion: String by project

dependencies {
    // The blaze project for annotations and packet
    implementation(project(":blaze"))

    // Symbol processing api for annotation processing
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    // Kotlin Poet for generating source code
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
}

