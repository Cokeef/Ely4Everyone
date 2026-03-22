package dev.ely4everyone.velocity.auth

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ReplayProtection {
    private val acceptedTickets = ConcurrentHashMap<String, Long>()

    fun tryAccept(ticketId: String, expiresAtEpochSeconds: Long, now: Instant = Instant.now()): Boolean {
        purgeExpired(now.epochSecond)
        return acceptedTickets.putIfAbsent(ticketId, expiresAtEpochSeconds) == null
    }

    private fun purgeExpired(nowEpochSeconds: Long) {
        acceptedTickets.entries.removeIf { (_, expiresAt) -> expiresAt <= nowEpochSeconds }
    }
}

