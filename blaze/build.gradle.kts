plugins {
    kotlin("jvm")
}

val nettyVersion: String by project

dependencies {
    // Logging implementation
    implementation(project(":logger"))

    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
