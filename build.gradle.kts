plugins {
    java
}

// Group and version; the jar name follows version
group = "com.example.escui"
version = "2.0.0"

repositories {
    mavenCentral()
    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")
    // PlaceholderAPI (compile-time only, currently unused)
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // Server runs Paper 26.2, so compile against that API.
    // compileOnly keeps it out of the jar — the server already provides it.
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    // Declared as a soft dependency, but nothing uses it yet.
    // Also compileOnly, so it isn't bundled.
    compileOnly("me.clip:placeholderapi:2.12.3")
}

java {
    // MC 26.x runs on Java 25
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    // Surface deprecated Paper APIs while there's still time to react
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    // Replace ${version} in paper-plugin.yml with the version above
    val props = mapOf("version" to version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.jar {
    // Produces e.g. EscUI-2.0.0.jar
    archiveBaseName.set("EscUI")
}
