package dev.ely4everyone.paper

data class PaperBridgeConfig(
    val enabled: Boolean,
    val logTrustedLogins: Boolean,
    val autoLoginDelayTicks: Long,
    val autoLoginCommand: String,
)

