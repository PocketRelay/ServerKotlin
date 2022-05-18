import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

val exposedVersion: String by project

dependencies {
    implementation("net.mamoe.yamlkt:yamlkt:0.10.2")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("io.netty:netty-all:4.1.76.Final")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation(project(":blaze"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}
