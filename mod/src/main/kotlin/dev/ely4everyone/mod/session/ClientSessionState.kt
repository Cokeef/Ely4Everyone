package dev.ely4everyone.mod.session

import java.time.Instant

data class ClientSessionState(
    val relayBaseUrl: String,
    val authSessionToken: String? = null,
    val elyAccessToken: String? = null,
    val authSessionExpiresAt: Instant? = null,
    val elyUsername: String? = null,
    val elyUuid: String? = null,
    val elyTexturesValue: String? = null,
    val elyTexturesSignature: String? = null,
) {
    fun hasUsableAuthSession(now: Instant = Instant.now()): Boolean {
        val expiresAt = authSessionExpiresAt ?: return false
        return !authSessionToken.isNullOrBlank() && expiresAt.isAfter(now)
    }

    fun hasUsableElyAccessToken(now: Instant = Instant.now()): Boolean {
        val expiresAt = authSessionExpiresAt ?: return false
        return !elyAccessToken.isNullOrBlank() && expiresAt.isAfter(now)
    }

    fun hasRestorableElyIdentity(now: Instant = Instant.now()): Boolean {
        return hasUsableElyAccessToken(now) &&
            !elyUsername.isNullOrBlank() &&
            !elyUuid.isNullOrBlank()
    }
}
