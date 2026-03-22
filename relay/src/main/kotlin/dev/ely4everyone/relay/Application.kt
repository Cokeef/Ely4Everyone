package dev.ely4everyone.relay

import dev.ely4everyone.relay.config.RelayConfig
import dev.ely4everyone.relay.routes.installRelayRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main() {
    val config = RelayConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, module = {
        module(config)
    }).start(wait = true)
}

fun Application.module(config: RelayConfig = RelayConfig.fromEnvironment()) {
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }

    installRelayRoutes(config)
}

