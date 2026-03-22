package dev.ely4everyone.velocity.auth.http

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class IssuedLoginTicketStore {
    private val tickets = ConcurrentHashMap<String, IssuedLoginTicketRecord>()

    fun put(record: IssuedLoginTicketRecord, now: Instant = Instant.now()) {
        purgeExpired(now)
        tickets[record.ticketId] = record
    }

    fun consume(ticketId: String, now: Instant = Instant.now()): IssuedLoginTicketRecord? {
        purgeExpired(now)
        return tickets.remove(ticketId)
    }

    private fun purgeExpired(now: Instant) {
        val epoch = now.epochSecond
        tickets.entries.removeIf { (_, record) -> record.expiresAtEpochSeconds <= epoch }
    }
}
