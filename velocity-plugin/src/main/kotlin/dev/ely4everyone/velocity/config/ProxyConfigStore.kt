package dev.ely4everyone.velocity.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream

object ProxyConfigStore {
    private const val FILE_NAME: String = "ely4everyone.properties"

    fun load(dataDirectory: Path): ProxyConfig {
        val configPath = dataDirectory.resolve(FILE_NAME)
        if (!Files.exists(configPath)) {
            return ProxyConfig()
        }

        val properties = Properties()
        configPath.inputStream().use(properties::load)
        return ProxyConfig.fromProperties(properties)
    }

    fun saveDefaultsIfMissing(dataDirectory: Path) {
        val configPath = dataDirectory.resolve(FILE_NAME)
        if (Files.exists(configPath)) {
            return
        }

        Files.createDirectories(dataDirectory)
        Files.newOutputStream(configPath).use { stream ->
            ProxyConfig().toProperties().store(stream, "Ely4Everyone Velocity config")
        }
    }
}
