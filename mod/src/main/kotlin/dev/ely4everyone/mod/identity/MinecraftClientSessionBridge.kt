package dev.ely4everyone.mod.identity

import dev.ely4everyone.mod.Ely4EveryoneClientMod
import dev.ely4everyone.mod.mixin.client.MinecraftClientAccessor
import net.minecraft.client.MinecraftClient
import net.minecraft.client.session.Session
import org.slf4j.LoggerFactory
import java.util.UUID

object MinecraftClientSessionBridge {
    private val logger = LoggerFactory.getLogger("${Ely4EveryoneClientMod.MOD_ID}/session-bridge")

    fun applyElyIdentity(identity: ElyIdentity): Boolean {
        val client = MinecraftClient.getInstance()
        val currentSession = client.session ?: return false
        val uuid = runCatching { UUID.fromString(identity.uuid) }.getOrNull() ?: return false

        val replacement = Session(
            identity.username,
            uuid,
            identity.accessToken,
            currentSession.xuid,
            currentSession.clientId,
        )

        (client as MinecraftClientAccessor).`ely4everyone$setSession`(replacement)
        logger.info("Applied Ely identity to MinecraftClient session. username={}, uuid={}", identity.username, identity.uuid)
        return true
    }

    fun currentSessionSummary(): String {
        val currentSession = MinecraftClient.getInstance().session ?: return "session=none"
        return "session(username=${currentSession.username}, uuid=${currentSession.uuidOrNull}, tokenPresent=${currentSession.accessToken.isNotBlank()})"
    }
}
