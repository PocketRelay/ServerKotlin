plugins {
    kotlin("jvm")
}

dependencies {
    // Logging implementation
    implementation(project(":logger"))

    val nettyVersion: String by project
    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
