package dev.ely4everyone.mod.config

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream

object ModConfigStore {
    private const val FILE_NAME: String = "ely4everyone.properties"

    fun load(): ModConfig {
        val configPath = resolveConfigPath()
        if (!Files.exists(configPath)) {
            return ModConfig()
        }

        val properties = Properties()
        configPath.inputStream().use(properties::load)
        return ModConfig.fromProperties(properties)
    }

    fun save(config: ModConfig) {
        val configPath = resolveConfigPath()
        Files.createDirectories(configPath.parent)
        Files.newOutputStream(configPath).use { stream ->
            config.toProperties().store(stream, "Ely4Everyone client config")
        }
    }

    fun resolveConfigPath(): Path {
        return FabricLoader.getInstance().configDir.resolve(FILE_NAME)
    }

    fun saveDefaultsIfMissing() {
        val configPath = resolveConfigPath()
        if (Files.exists(configPath)) {
            return
        }

        Files.createDirectories(configPath.parent)
        Files.newOutputStream(configPath).use { stream ->
            ModConfig().toProperties().store(stream, "Ely4Everyone client config")
        }
    }
}
