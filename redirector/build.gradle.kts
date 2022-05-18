apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":blaze"))
    implementation(project(":utils"))
}
