package dev.ely4everyone.relay.auth

enum class PendingAuthStatus {
    PENDING,
    COMPLETED,
    FAILED,
}

data class PendingAuthSession(
    val status: PendingAuthStatus,
    val state: String,
    val expiresAtEpochSeconds: Long,
    val clientRedirectUri: String? = null,
    val authSessionToken: String? = null,
    val elyAccessToken: String? = null,
    val username: String? = null,
    val uuid: String? = null,
    val error: String? = null,
)
