package dev.ely4everyone.velocity.auth.http

data class IssuedLoginTicketRecord(
    val ticketId: String,
    val username: String,
    val uuid: String,
    val expiresAtEpochSeconds: Long,
    val properties: List<AuthProfileProperty>,
)

