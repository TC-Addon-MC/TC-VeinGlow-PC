
plugins {
    id("dev.architectury.loom") version "1.7-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
}

val minecraft_version: String by project
val yarn_mappings: String by project
val neoforge_version: String by project
val cloth_config_version: String by project
val mod_version: String by project

version = mod_version

base { archivesName = "tc-veinglow-${minecraft_version}-neoforge" }

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    neoForge("net.neoforged:neoforge:${neoforge_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    
    // Cloth Config for NeoForge - mapped by loom
    modApi("me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}")
}

