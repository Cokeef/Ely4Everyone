package dev.ely4everyone.relay.auth

import java.io.Serializable

data class AuthProfileProperty(
    val name: String,
    val value: String,
    val signature: String? = null,
) : Serializable

