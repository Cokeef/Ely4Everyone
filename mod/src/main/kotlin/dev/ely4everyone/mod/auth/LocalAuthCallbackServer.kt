package dev.ely4everyone.mod.auth

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class LocalAuthCallbackServer private constructor(
    private val expectedState: String,
    private val server: HttpServer,
    val callbackUri: URI,
) {
    @Volatile
    private var result: LocalAuthCallbackPayload? = null

    fun consumeResult(): LocalAuthCallbackPayload? {
        val current = result
        result = null
        return current
    }

    fun stop() {
        server.stop(0)
    }

    companion object {
        fun start(expectedState: String): LocalAuthCallbackServer {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.executor = Executors.newSingleThreadExecutor()
            val instance = LocalAuthCallbackServer(
                expectedState = expectedState,
                server = server,
                callbackUri = URI.create("http://127.0.0.1:${server.address.port}/callback"),
            )

            server.createContext("/callback") { exchange ->
                instance.handleCallback(exchange)
            }
            server.start()
            return instance
        }
    }

    private fun handleCallback(exchange: HttpExchange) {
        val values = parseQuery(exchange.requestURI.rawQuery)
        val state = values["state"].orEmpty()
        val payload = LocalAuthCallbackPayload(
            state = state,
            status = values["status"].orEmpty(),
            authSessionToken = values["auth_session_token"],
            elyAccessToken = values["ely_access_token"],
            username = values["username"],
            uuid = values["uuid"],
            expiresAtEpochSeconds = values["exp"]?.toLongOrNull(),
            texturesValue = values["textures_value"],
            texturesSignature = values["textures_signature"],
            error = values["error"],
        )

        result = if (state == expectedState) payload else payload.copy(status = "failed", error = "state_mismatch")

        val body = if (result?.status == "completed") {
            """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="utf-8"><title>Ely4Everyone</title></head>
            <body style="font-family: sans-serif; margin: 2rem;">
                <h1>Ely4Everyone</h1>
                <p>Authorization completed for ${values["username"] ?: "unknown user"}.</p>
                <p>You can return to Minecraft.</p>
            </body>
            </html>
            """.trimIndent()
        } else {
            """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="utf-8"><title>Ely4Everyone</title></head>
            <body style="font-family: sans-serif; margin: 2rem;">
                <h1>Ely4Everyone</h1>
                <p>Authorization failed: ${result?.error ?: "unknown_error"}.</p>
                <p>You can return to Minecraft.</p>
            </body>
            </html>
            """.trimIndent()
        }.toByteArray(StandardCharsets.UTF_8)

        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, body.size.toLong())
        exchange.responseBody.use { output -> output.write(body) }
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) {
            return emptyMap()
        }

        val result = LinkedHashMap<String, String>()
        val parts = rawQuery.split('&')
        for (part in parts) {
            if (part.isBlank()) continue
            val separator = part.indexOf('=')
            if (separator < 0) {
                result[decode(part)] = ""
            } else {
                result[decode(part.substring(0, separator))] = decode(part.substring(separator + 1))
            }
        }
        return result
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
