plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft_version: String by project
val parchment_version: String by project
val parchment_mc_version: String by project
val cloth_config_version: String by project
val mod_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-client" }

architectury {
    common("fabric", "neoforge", "forge")
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

    implementation(project(path = ":common", configuration = "namedElements"))

    modCompileOnly("me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}")
}

configurations.create("commonJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

tasks.jar {
    archiveClassifier = "dev"
    from(project(":common").sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
