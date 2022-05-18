apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":blaze"))
    implementation(project(":utils"))
}

tasks.create("fatJar", Jar::class.java) {
    manifest {
        attributes["Implementation-Title"] = "KME Bundle Fat Jar"
        attributes["Main-Class"] = "com.jacobtread.kme.servers.RedirectServer"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}