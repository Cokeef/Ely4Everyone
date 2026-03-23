package dev.ely4everyone.velocity.auth.http

enum class PendingAuthStatus {
    PENDING,
    COMPLETED,
    FAILED,
}

data class PendingAuthSession(
    val status: PendingAuthStatus,
    val state: String,
    val expiresAtEpochSeconds: Long,
    val authSessionToken: String? = null,
    val elyAccessToken: String? = null,
    val username: String? = null,
    val uuid: String? = null,
    val texturesValue: String? = null,
    val texturesSignature: String? = null,
    val error: String? = null,
)
