plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft_version: String by project
val forge_version: String by project
val parchment_version: String by project
val parchment_mc_version: String by project
val cloth_config_version: String by project
val mod_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-${minecraft_version}-forge" }

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchment_mc_version}:${parchment_version}@zip")
    })
    "forge"("net.minecraftforge:forge:${minecraft_version}-${forge_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":client", configuration = "namedElements"))

    modApi("me.shedaniel.cloth:cloth-config-forge:${cloth_config_version}")
}
