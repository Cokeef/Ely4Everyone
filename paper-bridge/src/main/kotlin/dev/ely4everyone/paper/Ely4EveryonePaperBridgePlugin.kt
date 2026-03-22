package dev.ely4everyone.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class Ely4EveryonePaperBridgePlugin : JavaPlugin(), Listener {
    private lateinit var bridgeConfig: PaperBridgeConfig

    override fun onEnable() {
        saveDefaultConfig()
        reloadBridgeConfig()
        server.pluginManager.registerEvents(this, this)
        logger.info("Ely4Everyone Paper bridge enabled.")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!bridgeConfig.enabled) {
            return
        }

        val player = event.player
        if (!TrustedPlayerDetector.isTrustedForwardedUuid(player.name, player.uniqueId)) {
            return
        }

        if (bridgeConfig.logTrustedLogins) {
            logger.info("Detected trusted forwarded player ${player.name} with UUID ${player.uniqueId}. Scheduling backend auto-login command.")
        }

        if (bridgeConfig.autoLoginCommand.isBlank()) {
            return
        }

        val command = bridgeConfig.autoLoginCommand.replace("{player}", player.name)
        server.scheduler.runTaskLater(
            this,
            Runnable {
                server.dispatchCommand(server.consoleSender, command)
            },
            bridgeConfig.autoLoginDelayTicks,
        )
    }

    override fun reloadConfig() {
        super.reloadConfig()
        reloadBridgeConfig()
    }

    private fun reloadBridgeConfig() {
        bridgeConfig = PaperBridgeConfigLoader.load(this)
    }
}
