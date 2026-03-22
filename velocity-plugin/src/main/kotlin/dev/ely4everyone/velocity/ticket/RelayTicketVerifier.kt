package dev.ely4everyone.velocity.ticket

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class VerifiedTicket(
    val issuer: String,
    val audience: String,
    val subject: String,
    val username: String,
    val issuedAtEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val ticketId: String,
    val nonce: String,
)

object RelayTicketVerifier {
    fun verify(
        token: String,
        trustedIssuer: String,
        expectedAudience: String,
        signingKey: String,
        now: Instant = Instant.now(),
    ): VerifiedTicket? {
        val parts = token.split('.')
        if (parts.size != 2) {
            return null
        }

        val payloadPart = parts[0]
        val signaturePart = parts[1]
        val expectedSignature = sign(payloadPart, signingKey)
        if (expectedSignature != signaturePart) {
            return null
        }

        val payload = String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8)
        val values = payload.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) {
                    null
                } else {
                    line.substring(0, index) to line.substring(index + 1)
                }
            }
            .toMap()

        if (values["ver"] != "v1") {
            return null
        }

        val issuer = values["iss"] ?: return null
        val audience = values["aud"] ?: return null
        val subject = values["sub"] ?: return null
        val username = values["name"] ?: return null
        val issuedAt = values["iat"]?.toLongOrNull() ?: return null
        val expiresAt = values["exp"]?.toLongOrNull() ?: return null
        val ticketId = values["jti"] ?: return null
        val nonce = values["nonce"] ?: return null

        if (issuer != trustedIssuer) {
            return null
        }

        if (audience != expectedAudience) {
            return null
        }

        if (expiresAt <= now.epochSecond) {
            return null
        }

        if (issuedAt > now.plusSeconds(30).epochSecond) {
            return null
        }

        return VerifiedTicket(
            issuer = issuer,
            audience = audience,
            subject = subject,
            username = username,
            issuedAtEpochSeconds = issuedAt,
            expiresAtEpochSeconds = expiresAt,
            ticketId = ticketId,
            nonce = nonce,
        )
    }

    private fun sign(payloadPart: String, signingKey: String): String {
        val keySpec = SecretKeySpec(signingKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val signature = mac.doFinal(payloadPart.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }
}

