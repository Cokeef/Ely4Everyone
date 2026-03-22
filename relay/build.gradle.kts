plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val ktorVersion: String by project

base {
    archivesName.set("ely4everyone-relay")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

application {
    mainClass.set("dev.ely4everyone.relay.ApplicationKt")
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

