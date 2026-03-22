package dev.ely4everyone.relay.routes

import dev.ely4everyone.relay.auth.ClientAuthSessionStore
import dev.ely4everyone.relay.auth.ElyOAuthClient
import dev.ely4everyone.relay.auth.OAuthStateStore
import dev.ely4everyone.relay.auth.PendingAuthStatus
import dev.ely4everyone.relay.config.RelayConfig
import dev.ely4everyone.relay.protocol.TicketClaims
import dev.ely4everyone.relay.protocol.TicketCodec
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

fun Application.installRelayRoutes(config: RelayConfig) {
    val stateStore = OAuthStateStore(config.oauthStateTtlSeconds)
    val clientSessionStore = ClientAuthSessionStore(
        ttlSeconds = config.clientSessionTtlSeconds,
        storagePath = config.dataDirectory.resolve("client-auth-sessions.bin"),
    )
    val oauthClient = ElyOAuthClient(config)

    routing {
        get("/health") {
            call.respond(
                RelayHealthResponse(
                    status = "ok",
                    issuer = config.issuer,
                    publicBaseUrl = config.publicBaseUrl,
                ),
            )
        }

        route("/api/v1") {
            get("/config") {
                call.respond(
                    RelayConfigResponse(
                        issuer = config.issuer,
                        relayBaseUrl = config.publicBaseUrl,
                        audience = "local-network",
                        oauthEnabled = config.elyClientId.isNotBlank() && config.elyClientSecret.isNotBlank(),
                        devTicketsEnabled = config.allowInsecureDevTickets,
                    ),
                )
            }

            get("/auth/start") {
                val state = call.parameters["state"]
                if (state.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, RelayErrorResponse("missing_state"))
                    return@get
                }

                if (config.elyClientId.isBlank() || config.elyClientSecret.isBlank()) {
                    call.respond(HttpStatusCode.ServiceUnavailable, RelayErrorResponse("oauth_not_configured"))
                    return@get
                }

                stateStore.create(state, call.parameters["client_redirect_uri"])
                call.respondRedirect(oauthClient.buildAuthorizationUri(state).toString())
            }

            get("/auth/poll") {
                val state = call.parameters["state"]
                if (state.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, RelayPollResponse(status = "failed", error = "missing_state"))
                    return@get
                }

                val session = stateStore.get(state)
                if (session == null) {
                    call.respond(HttpStatusCode.NotFound, RelayPollResponse(status = "failed", error = "unknown_state"))
                    return@get
                }

                when (session.status) {
                    PendingAuthStatus.PENDING -> call.respond(RelayPollResponse(status = "pending"))
                    PendingAuthStatus.FAILED -> call.respond(RelayPollResponse(status = "failed", error = session.error))
                    PendingAuthStatus.COMPLETED -> call.respond(
                        RelayPollResponse(
                            status = "completed",
                            authSessionToken = session.authSessionToken,
                            elyAccessToken = session.elyAccessToken,
                            username = session.username,
                            uuid = session.uuid,
                            expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                        ),
                    )
                }
            }

            get("/auth/dev/latest-session") {
                val session = clientSessionStore.latest()
                if (session == null) {
                    call.respond(HttpStatusCode.NotFound, RelayPollResponse(status = "failed", error = "no_session"))
                    return@get
                }

                call.respond(
                    RelayPollResponse(
                        status = "completed",
                        authSessionToken = session.sessionToken,
                        elyAccessToken = session.elyAccessToken,
                        username = session.username,
                        uuid = session.uuid,
                        expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                    ),
                )
            }

            get("/auth/issue-ticket") {
                val sessionToken = call.parameters["session_token"]
                val nonce = call.parameters["nonce"]
                val audience = call.parameters["audience"] ?: "local-network"

                if (sessionToken.isNullOrBlank() || nonce.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, RelayPollResponse(status = "failed", error = "missing_session_token_or_nonce"))
                    return@get
                }

                val clientSession = clientSessionStore.get(sessionToken)
                if (clientSession == null) {
                    call.respond(HttpStatusCode.NotFound, RelayPollResponse(status = "failed", error = "invalid_session"))
                    return@get
                }

                val ticket = issueTicket(
                    config = config,
                    uuid = clientSession.uuid,
                    username = clientSession.username,
                    audience = audience,
                    nonce = nonce,
                )
                call.respond(ticket)
            }

            post("/dev/tickets") {
                if (!config.allowInsecureDevTickets) {
                    call.respond(HttpStatusCode.Forbidden, RelayErrorResponse("dev_tickets_disabled"))
                    return@post
                }

                val username = call.parameters["username"]
                val uuid = call.parameters["uuid"]
                val audience = call.parameters["audience"] ?: "local-network"
                val nonce = call.parameters["nonce"] ?: UUID.randomUUID().toString()

                if (username.isNullOrBlank() || uuid.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, RelayErrorResponse("missing_username_or_uuid"))
                    return@post
                }

                call.respond(
                    issueTicket(
                        config = config,
                        uuid = uuid,
                        username = username,
                        audience = audience,
                        nonce = nonce,
                    ),
                )
            }
        }

        get("/oauth/callback") {
            val state = call.parameters["state"]
            if (state.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, RelayErrorResponse("missing_state"))
                return@get
            }

            val existingSession = stateStore.get(state)

            val error = call.parameters["error"]
            if (!error.isNullOrBlank()) {
                stateStore.fail(state, call.parameters["error_message"] ?: error)
                if (!existingSession?.clientRedirectUri.isNullOrBlank()) {
                    call.respondRedirect(existingSession.clientRedirectUri + buildClientRedirectQuery(status = "failed", state = state, error = call.parameters["error_message"] ?: error))
                } else {
                    call.respondTextPage("Ely4Everyone", "Authorization failed: ${call.parameters["error_message"] ?: error}")
                }
                return@get
            }

            val code = call.parameters["code"]
            if (code.isNullOrBlank()) {
                stateStore.fail(state, "missing authorization code")
                call.respond(HttpStatusCode.BadRequest, RelayErrorResponse("missing_code"))
                return@get
            }

            runCatching {
                val tokenResponse = oauthClient.exchangeCode(code)
                val accountInfo = oauthClient.fetchAccountInfo(tokenResponse.accessToken)
                val properties = oauthClient.fetchTexturesProfile(accountInfo.uuid)
                val clientSession = clientSessionStore.create(
                    username = accountInfo.username,
                    uuid = accountInfo.uuid,
                    elyAccessToken = tokenResponse.accessToken,
                    properties = properties,
                )

                stateStore.complete(
                    state = state,
                    authSessionToken = clientSession.sessionToken,
                    elyAccessToken = clientSession.elyAccessToken,
                    username = accountInfo.username,
                    uuid = accountInfo.uuid,
                    expiresAtEpochSeconds = clientSession.expiresAtEpochSeconds,
                )
                val texturesProperty = properties.firstOrNull { it.name == "textures" }
                if (!existingSession?.clientRedirectUri.isNullOrBlank()) {
                    call.respondRedirect(
                        existingSession.clientRedirectUri + buildClientRedirectQuery(
                            status = "completed",
                            state = state,
                            authSessionToken = clientSession.sessionToken,
                            elyAccessToken = clientSession.elyAccessToken,
                            username = accountInfo.username,
                            uuid = accountInfo.uuid,
                            expiresAtEpochSeconds = clientSession.expiresAtEpochSeconds,
                            texturesValue = texturesProperty?.value,
                            texturesSignature = texturesProperty?.signature,
                        ),
                    )
                } else {
                    call.respondTextPage(
                        "Ely4Everyone",
                        "Authorization completed for ${accountInfo.username}. You can return to Minecraft and wait for the mod to finish polling.",
                    )
                }
            }.onFailure { exception ->
                stateStore.fail(state, exception.message ?: exception::class.java.simpleName)
                if (!existingSession?.clientRedirectUri.isNullOrBlank()) {
                    call.respondRedirect(existingSession.clientRedirectUri + buildClientRedirectQuery(status = "failed", state = state, error = exception.message ?: exception::class.java.simpleName))
                } else {
                    call.respondTextPage("Ely4Everyone", "Authorization failed: ${exception.message ?: exception::class.java.simpleName}")
                }
            }
        }
    }
}

private fun buildClientRedirectQuery(
    status: String,
    state: String,
    authSessionToken: String? = null,
    elyAccessToken: String? = null,
    username: String? = null,
    uuid: String? = null,
    expiresAtEpochSeconds: Long? = null,
    texturesValue: String? = null,
    texturesSignature: String? = null,
    error: String? = null,
): String {
    val parts = mutableListOf(
        "status" to status,
        "state" to state,
        "auth_session_token" to authSessionToken,
        "ely_access_token" to elyAccessToken,
        "username" to username,
        "uuid" to uuid,
        "exp" to expiresAtEpochSeconds?.toString(),
        "textures_value" to texturesValue,
        "textures_signature" to texturesSignature,
        "error" to error,
    ).filter { (_, value) -> !value.isNullOrBlank() }
        .joinToString("&") { (key, value) ->
            key + "=" + java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
        }

    return if (parts.isBlank()) "" else "?" + parts
}

private suspend fun io.ktor.server.application.ApplicationCall.respondTextPage(title: String, message: String) {
    respond(
        HttpStatusCode.OK,
        """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="utf-8"><title>$title</title></head>
        <body style="font-family: sans-serif; margin: 2rem;">
            <h1>$title</h1>
            <p>$message</p>
        </body>
        </html>
        """.trimIndent(),
    )
}

private fun issueTicket(
    config: RelayConfig,
    uuid: String,
    username: String,
    audience: String,
    nonce: String,
): RelayTicketResponse {
    val now = Instant.now()
    val expiresAt = now.plusSeconds(config.issuedTicketTtlSeconds).epochSecond
    val claims = TicketClaims(
        issuer = config.issuer,
        audience = audience,
        subject = uuid,
        username = username,
        issuedAtEpochSeconds = now.epochSecond,
        expiresAtEpochSeconds = expiresAt,
        ticketId = UUID.randomUUID().toString(),
        nonce = nonce,
    )
    return RelayTicketResponse(
        status = "completed",
        ticket = TicketCodec.encode(claims, config.ticketSigningKey),
        username = username,
        uuid = uuid,
        expiresAtEpochSeconds = expiresAt,
    )
}

@Serializable
data class RelayHealthResponse(
    val status: String,
    val issuer: String,
    val publicBaseUrl: String,
)

@Serializable
data class RelayConfigResponse(
    val issuer: String,
    val relayBaseUrl: String,
    val audience: String,
    val oauthEnabled: Boolean,
    val devTicketsEnabled: Boolean,
)

@Serializable
data class RelayPollResponse(
    val status: String,
    val authSessionToken: String? = null,
    val elyAccessToken: String? = null,
    val username: String? = null,
    val uuid: String? = null,
    val expiresAtEpochSeconds: Long? = null,
    val error: String? = null,
)

@Serializable
data class RelayTicketResponse(
    val status: String,
    val ticket: String,
    val username: String,
    val uuid: String,
    val expiresAtEpochSeconds: Long,
)

@Serializable
data class RelayErrorResponse(
    val error: String,
)
