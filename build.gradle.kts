plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(project(":TypeWriter-ProfilesExtension"))
}

group = "btc.renaud"
version = "0.0.3"


typewriter {
    namespace = "renaud"

    extension {
        name = "ArcheryGame"
        shortDescription = "Configurable archery minigame"
        description = """
            ArcheryGame is a TypeWriter extension for an archery minigame with multiple modes
            and extensive customization.
        """.trimIndent()
        engineVersion = file("../../version.txt").readText().trim()
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        dependencies {
            paper()
        }
    }
}

kotlin {
    jvmToolchain(21)
}
