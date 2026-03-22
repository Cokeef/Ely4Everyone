package dev.ely4everyone.mod.config

data class AuthServerPreset(
    val id: String,
    val label: String,
    val defaultUrl: String?,
)

object AuthServerPresets {
    const val LOCAL_VELOCITY: String = "local_velocity"
    const val HORNI_RELAY: String = "horni_relay"
    const val CUSTOM: String = "custom"

    val ALL: List<AuthServerPreset> = listOf(
        AuthServerPreset(
            id = LOCAL_VELOCITY,
            label = "Локальный Velocity",
            defaultUrl = "http://127.0.0.1:18085",
        ),
        AuthServerPreset(
            id = HORNI_RELAY,
            label = "horni.cc relay",
            defaultUrl = "https://horni.cc/auth/ely4everyone",
        ),
        AuthServerPreset(
            id = CUSTOM,
            label = "Свой auth-host",
            defaultUrl = null,
        ),
    )

    fun byId(id: String): AuthServerPreset {
        return ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}
