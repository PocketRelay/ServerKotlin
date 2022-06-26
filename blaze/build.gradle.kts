plugins {
    kotlin("jvm")
}

val nettyVersion: String by project

dependencies {
    // Utils project for logging
    implementation(project(":utils"))

    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
