plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val mod_version: String by project
val fabric_version: String by project
val cloth_config_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-client" }

architectury {
    common("fabric", "neoforge", "forge")
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    compileOnly("net.fabricmc:fabric-loader:${loader_version}")
    
    implementation(project(path = ":common", configuration = "namedElements"))
    
    // Config library is needed on client for GUI
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}")
}

configurations.create("commonJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

tasks.jar { archiveClassifier = "dev" }
