
plugins {
    id("dev.architectury.loom") version "1.7-SNAPSHOT"
    id("architectury-plugin") version "3.4-SNAPSHOT"
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project
val cloth_config_version: String by project
val modmenu_version: String by project
val mod_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-${minecraft_version}-fabric" }

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    
    modApi("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}")
    modLocalRuntime("com.terraformersmc:modmenu:${modmenu_version}")
}

