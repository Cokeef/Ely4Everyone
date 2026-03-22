package dev.ely4everyone.relay.auth

import java.io.Serializable

data class ClientAuthSession(
    val sessionToken: String,
    val username: String,
    val uuid: String,
    val elyAccessToken: String,
    val createdAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val properties: List<AuthProfileProperty>,
) : Serializable
