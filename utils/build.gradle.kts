plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val nettyVersion: String by project

dependencies {
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("io.netty:netty-buffer:$nettyVersion")
}