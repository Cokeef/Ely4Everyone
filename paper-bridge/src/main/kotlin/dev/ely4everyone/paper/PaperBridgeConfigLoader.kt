package dev.ely4everyone.paper

import org.bukkit.plugin.java.JavaPlugin

object PaperBridgeConfigLoader {
    fun load(plugin: JavaPlugin): PaperBridgeConfig {
        val config = plugin.config
        return PaperBridgeConfig(
            enabled = config.getBoolean("enabled", true),
            logTrustedLogins = config.getBoolean("log-trusted-logins", true),
            autoLoginDelayTicks = config.getLong("auto-login-delay-ticks", 20L),
            autoLoginCommand = config.getString("auto-login-command", "authme forcelogin {player}").orEmpty(),
        )
    }
}

