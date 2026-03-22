package dev.ely4everyone.velocity.protocol

import java.nio.charset.StandardCharsets

data class LoginChallenge(
    val version: String,
    val nonce: String,
    val audience: String,
)

data class LoginChallengeResponse(
    val ticket: String?,
)

object LoginChallengeCodec {
    fun encodeChallenge(challenge: LoginChallenge): ByteArray {
        val payload = buildString {
            appendLine("ver=${challenge.version}")
            appendLine("nonce=${challenge.nonce}")
            appendLine("aud=${challenge.audience}")
        }.trim()

        return payload.toByteArray(StandardCharsets.UTF_8)
    }

    fun decodeResponse(response: ByteArray?): LoginChallengeResponse {
        if (response == null || response.isEmpty()) {
            return LoginChallengeResponse(ticket = null)
        }

        val values = parseLines(String(response, StandardCharsets.UTF_8))
        return LoginChallengeResponse(ticket = values["ticket"]?.ifBlank { null })
    }

    private fun parseLines(payload: String): Map<String, String> {
        return payload.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            }
            .toMap()
    }
}
