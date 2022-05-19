group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

val exposedVersion: String by project

@Suppress("SpellCheckingInspection")
dependencies {
    // JDBC Connectors
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    // NOP to disable SLF4J
    implementation("org.slf4j:slf4j-nop:1.7.36")
    implementation(project(":blaze"))
    implementation(project(":utils"))
}


tasks.withType(Jar::class.java) {
    manifest {
        attributes["Main-Class"] = "com.jacobtread.kme.App"
    }
}