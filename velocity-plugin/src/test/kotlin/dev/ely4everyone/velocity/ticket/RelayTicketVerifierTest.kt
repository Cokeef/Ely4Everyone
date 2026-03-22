package dev.ely4everyone.velocity.ticket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RelayTicketVerifierTest {
    @Test
    fun `verifies valid issued ticket`() {
        val claims = IssuedTicketClaims(
            issuer = "issuer-a",
            audience = "network-a",
            subject = "123e4567-e89b-12d3-a456-426614174000",
            username = "NotchButEly",
            issuedAtEpochSeconds = 1_000,
            expiresAtEpochSeconds = 2_000,
            ticketId = "ticket-1",
            nonce = "nonce-1",
        )

        val token = IssuedTicketCodec.encode(claims, "secret")
        val verified = RelayTicketVerifier.verify(
            token = token,
            trustedIssuer = "issuer-a",
            expectedAudience = "network-a",
            signingKey = "secret",
            now = java.time.Instant.ofEpochSecond(1_500),
        )

        assertNotNull(verified)
        assertEquals("NotchButEly", verified.username)
        assertEquals("ticket-1", verified.ticketId)
        assertEquals("nonce-1", verified.nonce)
    }

    @Test
    fun `rejects expired ticket`() {
        val claims = IssuedTicketClaims(
            issuer = "issuer-a",
            audience = "network-a",
            subject = "123e4567-e89b-12d3-a456-426614174000",
            username = "Expired",
            issuedAtEpochSeconds = 1_000,
            expiresAtEpochSeconds = 1_100,
            ticketId = "ticket-2",
            nonce = "nonce-2",
        )

        val token = IssuedTicketCodec.encode(claims, "secret")
        val verified = RelayTicketVerifier.verify(
            token = token,
            trustedIssuer = "issuer-a",
            expectedAudience = "network-a",
            signingKey = "secret",
            now = java.time.Instant.ofEpochSecond(1_500),
        )

        assertNull(verified)
    }
}

