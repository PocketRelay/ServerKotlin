plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val nettyVersion: String by project
    val yamlktVersion: String by project

    implementation("net.mamoe.yamlkt:yamlkt:$yamlktVersion")
    implementation("io.netty:netty-buffer:$nettyVersion")
}