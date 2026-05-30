plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project
val cloth_config_version: String by project
val modmenu_version: String by project
val mod_version: String by project
val parchment_version: String by project
val parchment_mc_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-${minecraft_version}-fabric" }

architectury {
    platformSetupLoomIde()
    fabric()
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
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")

    implementation(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":client", configuration = "namedElements"))

    modApi("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}")
    modCompileOnlyApi("com.terraformersmc:modmenu:${modmenu_version}")
    modLocalRuntime("com.terraformersmc:modmenu:${modmenu_version}")
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    from(project(":common").sourceSets["main"].output)
    from(project(":client").sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
