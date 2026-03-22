package dev.ely4everyone.relay.auth

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClientAuthSessionStore(
    private val ttlSeconds: Long,
    private val storagePath: Path,
) {
    private val sessions = ConcurrentHashMap<String, ClientAuthSession>()

    init {
        loadFromDisk()
    }

    fun create(username: String, uuid: String, elyAccessToken: String, properties: List<AuthProfileProperty>, now: Instant = Instant.now()): ClientAuthSession {
        purgeExpired(now)
        val session = ClientAuthSession(
            sessionToken = UUID.randomUUID().toString(),
            username = username,
            uuid = uuid,
            elyAccessToken = elyAccessToken,
            createdAtEpochSeconds = now.epochSecond,
            expiresAtEpochSeconds = now.plusSeconds(ttlSeconds).epochSecond,
            properties = properties,
        )
        sessions[session.sessionToken] = session
        saveToDisk()
        return session
    }

    fun get(sessionToken: String, now: Instant = Instant.now()): ClientAuthSession? {
        purgeExpired(now)
        return sessions[sessionToken]
    }

    fun latest(now: Instant = Instant.now()): ClientAuthSession? {
        purgeExpired(now)
        return sessions.values.maxByOrNull { it.createdAtEpochSeconds }
    }

    private fun purgeExpired(now: Instant) {
        val epoch = now.epochSecond
        val changed = sessions.entries.removeIf { (_, session) -> session.expiresAtEpochSeconds <= epoch }
        if (changed) {
            saveToDisk()
        }
    }

    private fun loadFromDisk() {
        if (!Files.exists(storagePath)) {
            return
        }

        runCatching {
            ObjectInputStream(Files.newInputStream(storagePath)).use { input ->
                @Suppress("UNCHECKED_CAST")
                val restored = input.readObject() as? List<ClientAuthSession> ?: emptyList()
                restored.forEach { session ->
                    sessions[session.sessionToken] = session
                }
            }
        }
    }

    private fun saveToDisk() {
        Files.createDirectories(storagePath.parent)
        ObjectOutputStream(Files.newOutputStream(storagePath)).use { output ->
            output.writeObject(sessions.values.toList())
        }
    }
}
