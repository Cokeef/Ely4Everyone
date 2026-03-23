package dev.ely4everyone.mod.ui

import dev.ely4everyone.mod.auth.AuthFlowState
import dev.ely4everyone.mod.auth.AuthFlowStatus
import dev.ely4everyone.mod.auth.AuthWorkflowManager
import dev.ely4everyone.mod.config.AuthServerPresets
import dev.ely4everyone.mod.config.ModConfig
import dev.ely4everyone.mod.config.ModConfigStore
import dev.ely4everyone.mod.session.ClientSessionStore
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ElyAuthScreen(
    private val parent: Screen,
) : Screen(Text.literal("Ely4Everyone")) {
    private var workingConfig: ModConfig = ModConfigStore.load()
    private lateinit var authServerButton: ButtonWidget
    private lateinit var loginButton: ButtonWidget
    private lateinit var syncLatestSessionButton: ButtonWidget
    private lateinit var saveButton: ButtonWidget
    private lateinit var clearSessionButton: ButtonWidget
    private lateinit var backButton: ButtonWidget
    private lateinit var customAuthServerField: TextFieldWidget

    override fun init() {
        workingConfig = ModConfigStore.load()

        val layout = layout()

        customAuthServerField = TextFieldWidget(
            textRenderer,
            layout.left,
            layout.top + 57,
            layout.fieldWidth,
            20,
            Text.literal("Auth host URL"),
        ).apply {
            text = workingConfig.customAuthServerUrl
            setMaxLength(256)
            setEditable(workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM)
        }
        addDrawableChild(customAuthServerField)

        authServerButton = addDrawableChild(
            ButtonWidget.builder(Text.literal(serverButtonLabel())) {
                cycleAuthServer()
            }
                .dimensions(layout.left, layout.top + 28, layout.fieldWidth, 20)
                .build(),
        )

        loginButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Войти через Ely.by")) {
                saveWorkingConfig()
                AuthWorkflowManager.startBrowserLogin()
            }
                .dimensions(layout.left, layout.top + 91, layout.fieldWidth, 20)
                .build(),
        )

        syncLatestSessionButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Подтянуть сессию с auth-host")) {
                saveWorkingConfig()
                AuthWorkflowManager.syncLatestSessionFromAuthHost()
            }
                .dimensions(layout.left, layout.top + 117, layout.fieldWidth, 20)
                .build(),
        )

        saveButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Сохранить")) {
                saveWorkingConfig()
            }
                .dimensions(layout.left, layout.top + 143, 108, 20)
                .build(),
        )

        clearSessionButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Сбросить сессию")) {
                AuthWorkflowManager.clearSession()
            }
                .dimensions(layout.left + 112, layout.top + 143, 108, 20)
                .build(),
        )

        backButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Готово")) {
                saveWorkingConfig()
                client?.setScreen(parent)
            }
                .dimensions(layout.left, layout.top + 169, layout.fieldWidth, 20)
                .build(),
        )
    }

    override fun close() {
        saveWorkingConfig()
        client?.setScreen(parent)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0xF0121D20.toInt(), 0xF0171118.toInt())
        context.fill(0, 0, width, 28, 0xCC0A2B29.toInt())

        val layout = layout()
        drawPanel(
            context,
            layout.left - 12,
            layout.top - 18,
            layout.right + 12,
            layout.top + 198,
            0xCC15252A.toInt(),
            0xFF295B57.toInt(),
        )
        drawPanel(
            context,
            layout.left - 12,
            layout.infoTop,
            layout.right + 12,
            layout.infoBottom,
            0xCC132126.toInt(),
            0xFF214641.toInt(),
        )

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xF1FFF6.toInt())
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Ely.by авторизация, локальная Ely-сессия и auth-host настройки"),
            width / 2,
            31,
            0x86D9C4.toInt(),
        )

        authServerButton.render(context, mouseX, mouseY, delta)
        loginButton.render(context, mouseX, mouseY, delta)
        syncLatestSessionButton.render(context, mouseX, mouseY, delta)
        saveButton.render(context, mouseX, mouseY, delta)
        clearSessionButton.render(context, mouseX, mouseY, delta)
        backButton.render(context, mouseX, mouseY, delta)
        customAuthServerField.render(context, mouseX, mouseY, delta)

        val session = ClientSessionStore.load()
        val authState = AuthWorkflowManager.currentState()

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Источник авторизации"),
            layout.left,
            layout.top + 8,
            0x7EE7C8.toInt(),
        )
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Свой auth-host URL"),
            layout.left,
            layout.top + 38,
            if (workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM) 0xF1FFF6.toInt() else 0x6F8D85.toInt(),
        )

        val infoLines = listOf(
            "Auth-host: ${AuthServerPresets.byId(workingConfig.selectedAuthServerId).label}",
            "URL: ${workingConfig.resolvedAuthServerBaseUrl()}",
            "Ely user: ${session.elyUsername ?: "не авторизован"}",
            "UUID: ${session.elyUuid ?: "-"}",
            "Сессия: ${if (session.hasUsableAuthSession()) "активна" else "не активна"}",
        )

        infoLines.forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                layout.left - 4,
                layout.infoTop + 8 + index * 12,
                0xD7F2E8.toInt(),
            )
        }

        renderStatusCard(context, authState, layout.left - 4, layout.infoTop + 68, layout.right + 4)
    }

    private fun statusLabel(status: AuthFlowStatus): String {
        return when (status) {
            AuthFlowStatus.IDLE -> "Готов"
            AuthFlowStatus.OPENING_BROWSER -> "Открываю браузер"
            AuthFlowStatus.POLLING -> "Ожидание ответа"
            AuthFlowStatus.SUCCESS -> "Успешно"
            AuthFlowStatus.ERROR -> "Ошибка"
        }
    }

    private fun cycleAuthServer() {
        val currentIndex = AuthServerPresets.ALL.indexOfFirst { it.id == workingConfig.selectedAuthServerId }
        val nextPreset = AuthServerPresets.ALL[(currentIndex + 1).mod(AuthServerPresets.ALL.size)]
        workingConfig = workingConfig.copy(
            selectedAuthServerId = nextPreset.id,
            relayBaseUrl = nextPreset.defaultUrl ?: customAuthServerField.text.ifBlank { workingConfig.relayBaseUrl },
        )
        customAuthServerField.setEditable(nextPreset.id == AuthServerPresets.CUSTOM)
        authServerButton.message = Text.literal(serverButtonLabel())
    }

    private fun saveWorkingConfig() {
        val currentCustomUrl = customAuthServerField.text.ifBlank { workingConfig.customAuthServerUrl }
        val resolvedUrl = when (workingConfig.selectedAuthServerId) {
            AuthServerPresets.CUSTOM -> currentCustomUrl
            else -> AuthServerPresets.byId(workingConfig.selectedAuthServerId).defaultUrl ?: currentCustomUrl
        }

        workingConfig = workingConfig.copy(
            relayBaseUrl = resolvedUrl,
            customAuthServerUrl = currentCustomUrl,
        )
        ModConfigStore.save(workingConfig)
    }

    private fun serverButtonLabel(): String {
        return "Auth-host: " + AuthServerPresets.byId(workingConfig.selectedAuthServerId).label
    }

    private fun renderStatusCard(context: DrawContext, authState: AuthFlowState, left: Int, top: Int, right: Int) {
        val palette = when (authState.status) {
            AuthFlowStatus.SUCCESS -> Triple(0xCC163926.toInt(), 0xFF7CFF9A.toInt(), 0xE9FFF0.toInt())
            AuthFlowStatus.ERROR -> Triple(0xCC462022.toInt(), 0xFFFF9A9A.toInt(), 0xFFF1DADA.toInt())
            AuthFlowStatus.POLLING, AuthFlowStatus.OPENING_BROWSER -> Triple(0xCC213646.toInt(), 0xFF8ED7FF.toInt(), 0xFFE3F6FF.toInt())
            AuthFlowStatus.IDLE -> Triple(0xCC1E2830.toInt(), 0xFFD6E6EE.toInt(), 0xFFCFDCE3.toInt())
        }

        val bottom = top + 50
        drawPanel(context, left, top, right, bottom, palette.first, palette.second)
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Статус: ${statusLabel(authState.status)}"),
            left + 8,
            top + 7,
            palette.second,
        )

        wrapText(authState.message, right - left - 16).take(2).forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                left + 8,
                top + 21 + index * 11,
                palette.third,
            )
        }
    }

    private fun drawPanel(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, fillColor: Int, borderColor: Int) {
        context.fill(left, top, right, bottom, fillColor)
        context.fill(left, top, right, top + 1, borderColor)
        context.fill(left, bottom - 1, right, bottom, borderColor)
        context.fill(left, top, left + 1, bottom, borderColor)
        context.fill(right - 1, top, right, bottom, borderColor)
    }

    private fun wrapText(text: String, maxWidth: Int): List<String> {
        if (text.isBlank()) {
            return listOf("")
        }

        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotBlank()) {
                    lines += currentLine
                }
                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        return lines.ifEmpty { listOf(text) }
    }

    private fun layout(): ScreenLayout {
        val fieldWidth = 220
        val left = width / 2 - fieldWidth / 2
        val top = 44
        val infoTop = top + 214
        return ScreenLayout(
            left = left,
            right = left + fieldWidth,
            top = top,
            fieldWidth = fieldWidth,
            infoTop = infoTop,
            infoBottom = minOf(height - 20, infoTop + 84),
        )
    }
}

private data class ScreenLayout(
    val left: Int,
    val right: Int,
    val top: Int,
    val fieldWidth: Int,
    val infoTop: Int,
    val infoBottom: Int,
)
