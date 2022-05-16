import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.jacobtread.kme"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("io.netty:netty-all:4.1.76.Final")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation(project(":blaze"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.create("fatJar", Jar::class.java) {
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Main-Class"] = "com.jacobtread.kme.AppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

