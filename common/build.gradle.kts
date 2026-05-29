
plugins {
    id("dev.architectury.loom")
}
val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val mod_version: String by project
version = mod_version
base { archivesName = "tc-veinglow-common" }
loom {
    accessWidenerPath = file("src/main/resources/tc_veinminer.accesswidener")
}
dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    compileOnly("net.fabricmc:fabric-loader:${loader_version}")
    implementation("com.google.code.gson:gson:2.10.1")
}
configurations.create("commonJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}
tasks.jar { archiveClassifier = "dev" }

