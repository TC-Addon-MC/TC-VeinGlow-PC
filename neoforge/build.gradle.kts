plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft_version: String by project
val neoforge_version: String by project
val parchment_version: String by project
val parchment_mc_version: String by project
val cloth_config_version: String by project
val mod_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-${minecraft_version}-neoforge" }

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchment_mc_version}:${parchment_version}@zip")
    })
    "neoForge"("net.neoforged:neoforge:${neoforge_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":client", configuration = "namedElements"))

    modApi("me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}")
}
