group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":blaze"))
    implementation(project(":utils"))
}

tasks.withType(Jar::class.java) {
    manifest {
        attributes["Main-Class"] = "com.jacobtread.kme.servers.RedirectServer"
    }
}
