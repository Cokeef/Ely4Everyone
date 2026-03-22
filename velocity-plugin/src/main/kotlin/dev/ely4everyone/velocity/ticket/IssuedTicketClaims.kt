package dev.ely4everyone.velocity.ticket

data class IssuedTicketClaims(
    val issuer: String,
    val audience: String,
    val subject: String,
    val username: String,
    val issuedAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val ticketId: String,
    val nonce: String,
)

