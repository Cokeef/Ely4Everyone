package dev.ely4everyone.mod.config

import java.util.Properties

data class ModConfig(
    val relayBaseUrl: String = "http://127.0.0.1:18085",
    val selectedAuthServerId: String = AuthServerPresets.LOCAL_VELOCITY,
    val customAuthServerUrl: String = "http://127.0.0.1:18085",
    val serverDiscoveryMode: String = "auto",
    val preferredLoginMode: String = "ely-first",
) {
    fun toProperties(): Properties = Properties().also { props ->
        props.setProperty("relay_base_url", relayBaseUrl)
        props.setProperty("selected_auth_server_id", selectedAuthServerId)
        props.setProperty("custom_auth_server_url", customAuthServerUrl)
        props.setProperty("server_discovery_mode", serverDiscoveryMode)
        props.setProperty("preferred_login_mode", preferredLoginMode)
    }

    fun resolvedAuthServerBaseUrl(): String {
        val preset = AuthServerPresets.byId(selectedAuthServerId)
        return when (preset.id) {
            AuthServerPresets.CUSTOM -> customAuthServerUrl.ifBlank { relayBaseUrl }
            else -> preset.defaultUrl ?: relayBaseUrl
        }
    }

    companion object {
        fun fromProperties(properties: Properties): ModConfig = ModConfig(
            relayBaseUrl = properties.getProperty("relay_base_url", "http://127.0.0.1:18085"),
            selectedAuthServerId = properties.getProperty("selected_auth_server_id", AuthServerPresets.LOCAL_VELOCITY),
            customAuthServerUrl = properties.getProperty("custom_auth_server_url", "http://127.0.0.1:18085"),
            serverDiscoveryMode = properties.getProperty("server_discovery_mode", "auto"),
            preferredLoginMode = properties.getProperty("preferred_login_mode", "ely-first"),
        )
    }
}
