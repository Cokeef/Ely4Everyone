package dev.ely4everyone.velocity.ticket

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object IssuedTicketCodec {
    fun encode(claims: IssuedTicketClaims, signingKey: String): String {
        val payload = buildString {
            appendLine("ver=v1")
            appendLine("iss=${claims.issuer}")
            appendLine("aud=${claims.audience}")
            appendLine("sub=${claims.subject}")
            appendLine("name=${claims.username}")
            appendLine("iat=${claims.issuedAtEpochSeconds}")
            appendLine("exp=${claims.expiresAtEpochSeconds}")
            appendLine("jti=${claims.ticketId}")
            appendLine("nonce=${claims.nonce}")
        }.trim()

        val payloadPart = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        val signaturePart = sign(payloadPart, signingKey)
        return "$payloadPart.$signaturePart"
    }

    private fun sign(payloadPart: String, signingKey: String): String {
        val keySpec = SecretKeySpec(signingKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val signature = mac.doFinal(payloadPart.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }
}

