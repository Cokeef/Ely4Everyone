package dev.ely4everyone.velocity.auth.http

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class OAuthStateStore(
    private val ttlSeconds: Long,
) {
    private val sessions = ConcurrentHashMap<String, PendingAuthSession>()

    fun create(state: String, now: Instant = Instant.now()): PendingAuthSession {
        purgeExpired(now)
        val session = PendingAuthSession(
            status = PendingAuthStatus.PENDING,
            state = state,
            expiresAtEpochSeconds = now.plusSeconds(ttlSeconds).epochSecond,
        )
        sessions[state] = session
        return session
    }

    fun get(state: String, now: Instant = Instant.now()): PendingAuthSession? {
        purgeExpired(now)
        return sessions[state]
    }

    fun complete(
        state: String,
        authSessionToken: String,
        username: String,
        uuid: String,
        expiresAtEpochSeconds: Long,
    ): PendingAuthSession {
        val session = PendingAuthSession(
            status = PendingAuthStatus.COMPLETED,
            state = state,
            expiresAtEpochSeconds = expiresAtEpochSeconds,
            authSessionToken = authSessionToken,
            username = username,
            uuid = uuid,
        )
        sessions[state] = session
        return session
    }

    fun fail(state: String, error: String, now: Instant = Instant.now()): PendingAuthSession {
        val session = PendingAuthSession(
            status = PendingAuthStatus.FAILED,
            state = state,
            expiresAtEpochSeconds = now.plusSeconds(ttlSeconds).epochSecond,
            error = error,
        )
        sessions[state] = session
        return session
    }

    private fun purgeExpired(now: Instant) {
        val epoch = now.epochSecond
        sessions.entries.removeIf { (_, session) -> session.expiresAtEpochSeconds <= epoch }
    }
}
