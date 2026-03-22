package dev.ely4everyone.velocity.auth

import java.util.concurrent.ConcurrentHashMap

class TrustedLoginRegistry {
    private val trustedLogins = ConcurrentHashMap<String, TrustedLogin>()

    fun put(username: String, trustedLogin: TrustedLogin) {
        trustedLogins[normalize(username)] = trustedLogin
    }

    fun take(username: String): TrustedLogin? {
        return trustedLogins.remove(normalize(username))
    }

    private fun normalize(username: String): String {
        return username.lowercase()
    }
}

