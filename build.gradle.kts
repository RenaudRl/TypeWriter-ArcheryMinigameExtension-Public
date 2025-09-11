repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
}

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.0.0"
}

group = "btc.renaud"
version = "0.0.1"


typewriter {
    namespace = "renaud"

    extension {
        name = "ArcheryGame"
        shortDescription = "Configurable archery minigame"
        description = """
            ArcheryGame is a TypeWriter extension for an archery minigame with multiple modes
            and extensive customization.
        """.trimIndent()
        engineVersion = "0.9.0-beta-165"
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        dependencies {
            paper()
        }
    }
}

kotlin {
    jvmToolchain(21)
}