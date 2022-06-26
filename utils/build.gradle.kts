plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val yamlktVersion: String by project

    implementation("net.mamoe.yamlkt:yamlkt:$yamlktVersion")
}