plugins {
    id("fabric-loom")
    kotlin("jvm")
}

val minecraftVersion: String by project
val yarnMappings: String by project
val fabricLoaderVersion: String by project
val fabricApiVersion: String by project

base {
    archivesName.set("ely4everyone-mod")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        configurations.runtimeClasspath.get()
            .filter { file ->
                file.name.endsWith(".jar") && (
                    file.name.startsWith("kotlin-stdlib") ||
                        file.name.startsWith("annotations-")
                    )
            }
            .map { zipTree(it) },
    )
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
