package dev.ely4everyone.mod.auth

data class LocalAuthCallbackPayload(
    val state: String,
    val status: String,
    val authSessionToken: String? = null,
    val elyAccessToken: String? = null,
    val username: String? = null,
    val uuid: String? = null,
    val expiresAtEpochSeconds: Long? = null,
    val texturesValue: String? = null,
    val texturesSignature: String? = null,
    val error: String? = null,
)
