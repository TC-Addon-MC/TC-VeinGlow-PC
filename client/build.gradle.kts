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
val parchment_version: String by project
val parchment_mc_version: String by project

version = mod_version
base { archivesName = "tc-veinglow-client" }

architectury {
    common("fabric", "neoforge", "forge")
}

loom {
    accessWidenerPath = project(":common").file("src/main/resources/tc_veinminer.accesswidener")
    runs {
        // dùng Yarn cho dev, Mojang cho production transform
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:1.21+build.4")
    })
    compileOnly("net.fabricmc:fabric-loader:${loader_version}")

    implementation(project(path = ":common", configuration = "namedElements"))

    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}")
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