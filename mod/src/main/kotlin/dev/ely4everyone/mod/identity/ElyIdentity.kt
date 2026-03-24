package dev.ely4everyone.mod.identity

data class ElyIdentity(
    val username: String,
    val uuid: String,
    val accessToken: String,
    val texturesValue: String? = null,
    val texturesSignature: String? = null,
)

