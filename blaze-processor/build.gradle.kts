plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":blaze"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.0-1.0.6")
}

