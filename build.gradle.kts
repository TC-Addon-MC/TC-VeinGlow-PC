plugins {
    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
}
// Root build.gradle.kts — shared toolchain, repositories, and conventions
// Each subproject (common, fabric, neoforge, forge) has its own build.gradle.kts

subprojects {
    apply(plugin = "java")

    group = "com.tcveinminer"
    version = property("mod_version") as String

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://maven.parchmentmc.org")
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
        options.encoding = "UTF-8"
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
    }

    tasks.withType<ProcessResources>().configureEach {
        val replaceProperties = mapOf(
            "version" to project.property("mod_version") as String
        )
        inputs.properties(replaceProperties)

        filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "fabric.mod.json")) {
            expand(replaceProperties)
        }
    }
}
