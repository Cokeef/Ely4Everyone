package dev.ely4everyone.mod.identity

import dev.ely4everyone.mod.session.ClientSessionState

object ElyIdentityManager {
    fun fromClientSession(sessionState: ClientSessionState): ElyIdentity? {
        val username = sessionState.elyUsername ?: return null
        val uuid = sessionState.elyUuid ?: return null
        val accessToken = sessionState.elyAccessToken ?: return null

        return ElyIdentity(
            username = username,
            uuid = uuid,
            accessToken = accessToken,
        )
    }
}
