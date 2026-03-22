package dev.ely4everyone.velocity.auth.http

import java.io.Serializable

data class ClientAuthSession(
    val sessionToken: String,
    val username: String,
    val uuid: String,
    val createdAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val properties: List<AuthProfileProperty>,
) : Serializable
