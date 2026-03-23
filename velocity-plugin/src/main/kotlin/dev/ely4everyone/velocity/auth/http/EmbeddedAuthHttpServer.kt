package dev.ely4everyone.velocity.auth.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.ely4everyone.velocity.config.ProxyConfig
import dev.ely4everyone.velocity.ticket.IssuedTicketClaims
import dev.ely4everyone.velocity.ticket.IssuedTicketCodec
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EmbeddedAuthHttpServer(
    private val config: ProxyConfig,
    private val logger: Logger,
    dataDirectory: Path,
) {
    private val stateStore = OAuthStateStore(config.oauthStateTtlSeconds)
    private val clientSessionStore = ClientAuthSessionStore(
        ttlSeconds = config.clientSessionTtlSeconds,
        storagePath = dataDirectory.resolve("client-auth-sessions.bin"),
    )
    private val issuedLoginTicketStore = IssuedLoginTicketStore()
    private val oauthClient = ElyOAuthClient(config)
    private var executorService: ExecutorService? = null
    private var server: HttpServer? = null

    fun start() {
        if (!config.embeddedAuthEnabled) {
            logger.info("Embedded Ely auth host is disabled in config.")
            return
        }

        val address = InetSocketAddress(config.embeddedAuthBindHost, config.embeddedAuthBindPort)
        executorService = Executors.newCachedThreadPool()
        server = HttpServer.create(address, 0).apply {
            executor = executorService
            createContext("/health") { exchange -> handleHealth(exchange) }
            createContext("/api/v1/config") { exchange -> handleConfig(exchange) }
            createContext("/api/v1/auth/start") { exchange -> handleAuthStart(exchange) }
            createContext("/api/v1/auth/poll") { exchange -> handleAuthPoll(exchange) }
            createContext("/api/v1/auth/dev/latest-session") { exchange -> handleLatestSession(exchange) }
            createContext("/api/v1/auth/issue-ticket") { exchange -> handleIssueTicket(exchange) }
            createContext("/api/v1/dev/tickets") { exchange -> handleDevTickets(exchange) }
            createContext("/oauth/callback") { exchange -> handleOAuthCallback(exchange) }
            start()
        }

        logger.info(
            "Embedded Ely auth host started on {}:{} (publicBaseUrl={})",
            config.embeddedAuthBindHost,
            config.embeddedAuthBindPort,
            config.publicBaseUrl,
        )
    }

    fun stop() {
        server?.stop(0)
        server = null
        executorService?.shutdownNow()
        executorService = null
    }

    fun consumeIssuedTicketRecord(ticketId: String): IssuedLoginTicketRecord? {
        return issuedLoginTicketStore.consume(ticketId)
    }

    private fun handleHealth(exchange: HttpExchange) {
        writeText(
            exchange = exchange,
            statusCode = 200,
            contentType = "text/plain; charset=utf-8",
            body = """
                status=ok
                issuer=${config.trustedIssuer}
                oauth_enabled=${isOAuthConfigured()}
                embedded_auth_enabled=${config.embeddedAuthEnabled}
            """.trimIndent(),
        )
    }

    private fun handleConfig(exchange: HttpExchange) {
        writeText(
            exchange = exchange,
            statusCode = 200,
            contentType = "text/plain; charset=utf-8",
            body = """
                issuer=${config.trustedIssuer}
                relay_base_url=${config.publicBaseUrl}
                audience=${config.expectedAudience}
                oauth_enabled=${isOAuthConfigured()}
                dev_tickets_enabled=true
            """.trimIndent(),
        )
    }

    private fun handleAuthStart(exchange: HttpExchange) {
        val params = requestParameters(exchange)
        val state = params["state"]
        if (state.isNullOrBlank()) {
            writeText(exchange, 400, "text/plain; charset=utf-8", "error=missing_state")
            return
        }

        if (!isOAuthConfigured()) {
            writeText(
                exchange,
                503,
                "text/plain; charset=utf-8",
                "error=oauth_not_configured\nmessage=Set ely_client_id and ely_client_secret in plugin config.",
            )
            return
        }

        stateStore.create(state)
        redirect(exchange, oauthClient.buildAuthorizationUri(state))
    }

    private fun handleAuthPoll(exchange: HttpExchange) {
        val params = requestParameters(exchange)
        val state = params["state"]
        if (state.isNullOrBlank()) {
            writeText(exchange, 400, "text/plain; charset=utf-8", "status=failed\nerror=missing_state")
            return
        }

        val session = stateStore.get(state)
        if (session == null) {
            writeText(exchange, 404, "text/plain; charset=utf-8", "status=failed\nerror=unknown_state")
            return
        }

        val body = when (session.status) {
            PendingAuthStatus.PENDING -> "status=pending"
            PendingAuthStatus.FAILED -> "status=failed\nerror=${session.error.orEmpty()}"
            PendingAuthStatus.COMPLETED -> """
                status=completed
                auth_session_token=${session.authSessionToken.orEmpty()}
                ely_access_token=${session.elyAccessToken.orEmpty()}
                username=${session.username.orEmpty()}
                uuid=${session.uuid.orEmpty()}
                exp=${session.expiresAtEpochSeconds}
                textures_value=${session.texturesValue.orEmpty()}
                textures_signature=${session.texturesSignature.orEmpty()}
            """.trimIndent()
        }

        writeText(exchange, 200, "text/plain; charset=utf-8", body)
    }

    private fun handleIssueTicket(exchange: HttpExchange) {
        val params = requestParameters(exchange)
        val sessionToken = params["session_token"]
        val nonce = params["nonce"]
        val audience = params["audience"] ?: config.expectedAudience

        if (sessionToken.isNullOrBlank() || nonce.isNullOrBlank()) {
            writeText(exchange, 400, "text/plain; charset=utf-8", "status=failed\nerror=missing_session_token_or_nonce")
            return
        }

        val clientSession = clientSessionStore.get(sessionToken)
        if (clientSession == null) {
            writeText(exchange, 404, "text/plain; charset=utf-8", "status=failed\nerror=invalid_session")
            return
        }

        val issued = issueTicket(
            uuid = clientSession.uuid,
            username = clientSession.username,
            audience = audience,
            nonce = nonce,
            properties = clientSession.properties,
        )

        writeText(
            exchange = exchange,
            statusCode = 200,
            contentType = "text/plain; charset=utf-8",
            body = """
                status=completed
                ticket=${issued.ticket}
                username=${clientSession.username}
                uuid=${clientSession.uuid}
                exp=${issued.expiresAtEpochSeconds}
            """.trimIndent(),
        )
    }

    private fun handleLatestSession(exchange: HttpExchange) {
        val session = clientSessionStore.latest()
        if (session == null) {
            writeText(exchange, 404, "text/plain; charset=utf-8", "status=failed\nerror=no_session")
            return
        }

        writeText(
            exchange = exchange,
            statusCode = 200,
            contentType = "text/plain; charset=utf-8",
            body = """
                status=completed
                auth_session_token=${session.sessionToken}
                ely_access_token=${session.elyAccessToken}
                username=${session.username}
                uuid=${session.uuid}
                exp=${session.expiresAtEpochSeconds}
                textures_value=${session.properties.firstOrNull { it.name == "textures" }?.value.orEmpty()}
                textures_signature=${session.properties.firstOrNull { it.name == "textures" }?.signature.orEmpty()}
            """.trimIndent(),
        )
    }

    private fun handleDevTickets(exchange: HttpExchange) {
        val params = requestParameters(exchange)
        val username = params["username"]
        val uuid = params["uuid"]
        val audience = params["audience"] ?: config.expectedAudience
        val nonce = params["nonce"] ?: UUID.randomUUID().toString()

        if (username.isNullOrBlank() || uuid.isNullOrBlank()) {
            writeText(exchange, 400, "text/plain; charset=utf-8", "error=missing_username_or_uuid")
            return
        }

        val issued = issueTicket(
            uuid = uuid,
            username = username,
            audience = audience,
            nonce = nonce,
            properties = emptyList(),
        )

        writeText(
            exchange = exchange,
            statusCode = 200,
            contentType = "text/plain; charset=utf-8",
            body = """
                status=completed
                ticket=${issued.ticket}
                username=$username
                uuid=$uuid
                exp=${issued.expiresAtEpochSeconds}
            """.trimIndent(),
        )
    }

    private fun handleOAuthCallback(exchange: HttpExchange) {
        val params = requestParameters(exchange)
        val state = params["state"]
        if (state.isNullOrBlank()) {
            writeHtml(exchange, 400, "<h1>Ely4Everyone</h1><p>Missing OAuth state.</p>")
            return
        }

        if (params["error"] != null) {
            val error = params["error_message"] ?: params["error"].orEmpty()
            stateStore.fail(state, error)
            writeHtml(exchange, 400, "<h1>Ely4Everyone</h1><p>Authorization failed: ${escapeHtml(error)}</p>")
            return
        }

        val code = params["code"]
        if (code.isNullOrBlank()) {
            stateStore.fail(state, "missing authorization code")
            writeHtml(exchange, 400, "<h1>Ely4Everyone</h1><p>Authorization code is missing.</p>")
            return
        }

        runCatching {
            val tokenResponse = oauthClient.exchangeCode(code)
            val accountInfo = oauthClient.fetchAccountInfo(tokenResponse.accessToken)
            val texturesProfile = oauthClient.fetchTexturesProfile(accountInfo.uuid)
            val texturesProperty = texturesProfile.properties.firstOrNull { it.name == "textures" }
            val clientSession = clientSessionStore.create(
                username = accountInfo.username,
                uuid = accountInfo.uuid,
                elyAccessToken = tokenResponse.accessToken,
                properties = texturesProfile.properties,
            )

            stateStore.complete(
                state = state,
                authSessionToken = clientSession.sessionToken,
                elyAccessToken = clientSession.elyAccessToken,
                username = accountInfo.username,
                uuid = accountInfo.uuid,
                expiresAtEpochSeconds = clientSession.expiresAtEpochSeconds,
                texturesValue = texturesProperty?.value,
                texturesSignature = texturesProperty?.signature,
            )

            writeHtml(
                exchange,
                200,
                """
                    <h1>Ely4Everyone</h1>
                    <p>Authorization completed for <strong>${escapeHtml(accountInfo.username)}</strong>.</p>
                    <p>You can return to Minecraft and wait for the mod to finish polling.</p>
                """.trimIndent(),
            )
        }.onFailure { exception ->
            val error = exception.message ?: exception::class.java.simpleName
            stateStore.fail(state, error)
            logger.warn("Failed to complete Ely OAuth callback for state {}", state, exception)
            writeHtml(exchange, 500, "<h1>Ely4Everyone</h1><p>Authorization failed: ${escapeHtml(error)}</p>")
        }
    }

    private fun issueTicket(
        uuid: String,
        username: String,
        audience: String,
        nonce: String,
        properties: List<AuthProfileProperty>,
    ): IssuedTicketResult {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(config.issuedTicketTtlSeconds).epochSecond
        val ticketId = UUID.randomUUID().toString()
        val claims = IssuedTicketClaims(
            issuer = config.trustedIssuer,
            audience = audience,
            subject = uuid,
            username = username,
            issuedAtEpochSeconds = now.epochSecond,
            expiresAtEpochSeconds = expiresAt,
            ticketId = ticketId,
            nonce = nonce,
        )

        issuedLoginTicketStore.put(
            IssuedLoginTicketRecord(
                ticketId = ticketId,
                username = username,
                uuid = uuid,
                expiresAtEpochSeconds = expiresAt,
                properties = properties,
            ),
            now = now,
        )

        return IssuedTicketResult(
            ticket = IssuedTicketCodec.encode(claims, config.ticketSigningKey),
            expiresAtEpochSeconds = expiresAt,
        )
    }

    private fun requestParameters(exchange: HttpExchange): Map<String, String> {
        val queryParams = parseQuery(exchange.requestURI.rawQuery)
        if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
            val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            val merged = LinkedHashMap<String, String>()
            merged.putAll(queryParams)
            merged.putAll(parseQuery(body))
            return merged
        }

        return queryParams
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) {
            return emptyMap()
        }

        val result = LinkedHashMap<String, String>()
        var segmentStart = 0

        while (segmentStart <= rawQuery.length) {
            val separatorIndex = rawQuery.indexOf('&', segmentStart).let { if (it >= 0) it else rawQuery.length }
            val part = rawQuery.substring(segmentStart, separatorIndex)

            if (part.isNotBlank()) {
                val keyValueSeparator = part.indexOf('=')
                if (keyValueSeparator < 0) {
                    result[decodeUrl(part)] = ""
                } else {
                    val key = decodeUrl(part.substring(0, keyValueSeparator))
                    val value = decodeUrl(part.substring(keyValueSeparator + 1))
                    result[key] = value
                }
            }

            if (separatorIndex >= rawQuery.length) {
                break
            }

            segmentStart = separatorIndex + 1
        }

        return result
    }

    private fun writeText(exchange: HttpExchange, statusCode: Int, contentType: String, body: String) {
        writeResponse(exchange, statusCode, contentType, body.toByteArray(StandardCharsets.UTF_8))
    }

    private fun writeHtml(exchange: HttpExchange, statusCode: Int, body: String) {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="utf-8"><title>Ely4Everyone</title></head>
            <body style="font-family: sans-serif; margin: 2rem;">$body</body>
            </html>
        """.trimIndent()
        writeResponse(exchange, statusCode, "text/html; charset=utf-8", html.toByteArray(StandardCharsets.UTF_8))
    }

    private fun redirect(exchange: HttpExchange, uri: URI) {
        exchange.responseHeaders.add("Location", uri.toString())
        exchange.sendResponseHeaders(302, -1)
        exchange.close()
    }

    private fun writeResponse(exchange: HttpExchange, statusCode: Int, contentType: String, body: ByteArray) {
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(statusCode, body.size.toLong())
        exchange.responseBody.use { output ->
            output.write(body)
        }
    }

    private fun decodeUrl(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun isOAuthConfigured(): Boolean {
        return config.elyClientId.isNotBlank() && config.elyClientSecret.isNotBlank()
    }
}

data class IssuedTicketResult(
    val ticket: String,
    val expiresAtEpochSeconds: Long,
)
