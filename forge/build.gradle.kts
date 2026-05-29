
plugins {
    id("dev.architectury.loom") version "1.7-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
}

val minecraft_version: String by project
val yarn_mappings: String by project
val forge_version: String by project
val cloth_config_version: String by project
val mod_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-${minecraft_version}-forge" }

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath = file("src/main/resources/tc_veinminer.accesswidener")
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    forge("net.minecraftforge:forge:${minecraft_version}-${forge_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    
    // Cloth Config for Forge
    modApi("me.shedaniel.cloth:cloth-config-forge:${cloth_config_version}")
}

