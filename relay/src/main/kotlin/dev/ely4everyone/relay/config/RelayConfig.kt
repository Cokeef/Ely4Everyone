package dev.ely4everyone.relay.config

import java.nio.file.Path

data class RelayConfig(
    val issuer: String,
    val publicBaseUrl: String,
    val elyClientId: String,
    val elyClientSecret: String,
    val ticketSigningKey: String,
    val allowInsecureDevTickets: Boolean,
    val oauthScopes: String,
    val oauthStateTtlSeconds: Long,
    val clientSessionTtlSeconds: Long,
    val issuedTicketTtlSeconds: Long,
    val port: Int,
    val dataDirectory: Path,
) {
    companion object {
        fun fromEnvironment(): RelayConfig = RelayConfig(
            issuer = env("E4E_ISSUER", "local-dev"),
            publicBaseUrl = env("E4E_PUBLIC_BASE_URL", "http://localhost:8080"),
            elyClientId = env("ELY_CLIENT_ID", ""),
            elyClientSecret = env("ELY_CLIENT_SECRET", ""),
            ticketSigningKey = env("TICKET_SIGNING_KEY", "change-me"),
            allowInsecureDevTickets = env("E4E_ALLOW_INSECURE_DEV_TICKETS", "false").toBooleanStrictOrNull() ?: false,
            oauthScopes = env("E4E_OAUTH_SCOPES", "account_info minecraft_server_session"),
            oauthStateTtlSeconds = env("E4E_OAUTH_STATE_TTL_SECONDS", "300").toLongOrNull() ?: 300L,
            clientSessionTtlSeconds = env("E4E_CLIENT_SESSION_TTL_SECONDS", "86400").toLongOrNull() ?: 86400L,
            issuedTicketTtlSeconds = env("E4E_ISSUED_TICKET_TTL_SECONDS", "300").toLongOrNull() ?: 300L,
            port = env("PORT", "8080").toIntOrNull() ?: 8080,
            dataDirectory = Path.of(env("E4E_DATA_DIR", "./relay-data")),
        )

        private fun env(name: String, defaultValue: String): String {
            return System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue
        }
    }
}
