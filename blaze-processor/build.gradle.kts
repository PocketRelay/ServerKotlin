plugins {
    kotlin("jvm")
}


val nettyVersion: String by project

dependencies {
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation(project(":utils"))
    implementation(project(":blaze"))

    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.0-1.0.6")

    // Kotlin Poet
    val kotlinPoetVersion = "1.12.0"
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
}

