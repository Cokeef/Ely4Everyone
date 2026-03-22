package dev.ely4everyone.velocity.auth

import dev.ely4everyone.velocity.auth.http.AuthProfileProperty
import dev.ely4everyone.velocity.ticket.VerifiedTicket
import java.util.UUID

data class TrustedLogin(
    val uuid: UUID,
    val ticket: VerifiedTicket,
    val properties: List<AuthProfileProperty> = emptyList(),
)
