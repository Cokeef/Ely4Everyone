package dev.ely4everyone.mod.ui

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
        val centerX = width / 2
        val baseY = height / 4 - 6
        val buttonLeft = centerX - 100

        customAuthServerField = TextFieldWidget(
            textRenderer,
            buttonLeft,
            baseY + 34,
            200,
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
                .dimensions(buttonLeft, baseY + 8, 200, 20)
                .build(),
        )

        loginButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Войти через Ely.by")) {
                saveWorkingConfig()
                AuthWorkflowManager.startBrowserLogin()
            }
                .dimensions(buttonLeft, baseY + 62, 200, 20)
                .build(),
        )

        syncLatestSessionButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Подтянуть последнюю сессию")) {
                saveWorkingConfig()
                AuthWorkflowManager.syncLatestSessionFromAuthHost()
            }
                .dimensions(buttonLeft, baseY + 88, 200, 20)
                .build(),
        )

        saveButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Сохранить auth-host")) {
                saveWorkingConfig()
            }
                .dimensions(buttonLeft, baseY + 114, 200, 20)
                .build(),
        )

        clearSessionButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Очистить локальную сессию")) {
                AuthWorkflowManager.clearSession()
            }
                .dimensions(buttonLeft, baseY + 140, 200, 20)
                .build(),
        )

        backButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Назад")) {
                saveWorkingConfig()
                client?.setScreen(parent)
            }
                .dimensions(buttonLeft, baseY + 166, 200, 20)
                .build(),
        )
    }

    override fun close() {
        saveWorkingConfig()
        client?.setScreen(parent)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xCC101418.toInt())
        authServerButton.render(context, mouseX, mouseY, delta)
        loginButton.render(context, mouseX, mouseY, delta)
        syncLatestSessionButton.render(context, mouseX, mouseY, delta)
        saveButton.render(context, mouseX, mouseY, delta)
        clearSessionButton.render(context, mouseX, mouseY, delta)
        backButton.render(context, mouseX, mouseY, delta)
        customAuthServerField.render(context, mouseX, mouseY, delta)

        val session = ClientSessionStore.load()
        val authState = AuthWorkflowManager.currentState()

        val infoLines = listOf(
            "Выбранный auth-host: ${AuthServerPresets.byId(workingConfig.selectedAuthServerId).label}",
            "Текущий auth URL: ${workingConfig.resolvedAuthServerBaseUrl()}",
            "Текущий Ely user: ${session.elyUsername ?: "-"}",
            "Текущий Ely UUID: ${session.elyUuid ?: "-"}",
            "Auth session активна: ${if (session.hasUsableAuthSession()) "да" else "нет"}",
            "Статус: ${statusLabel(authState.status)}",
            authState.message,
        )

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4, 0xFFFFFF)
        infoLines.forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                width / 2 - 100,
                height / 4 + 184 + index * 12,
                0xD0D0D0,
            )
        }

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Сервер авторизации"),
            width / 2 - 100,
            height / 4,
            0x7CFF9A,
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Custom URL"),
            width / 2 - 100,
            height / 4 + 20,
            if (workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM) 0xFFFFFF else 0x888888,
        )

        if (authState.status == AuthFlowStatus.SUCCESS) {
            val panelLeft = width / 2 - 110
            val panelTop = minOf(height - 52, height / 4 + 146)
            val panelRight = width / 2 + 110
            val panelBottom = panelTop + 38

            context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC143D24.toInt())
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Авторизация успешна").styled { it.withColor(0x7CFF9A).withBold(true) },
                width / 2,
                panelTop + 6,
                0x7CFF9A,
            )
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(authState.message),
                width / 2,
                panelTop + 18,
                0xE6FFE9,
            )
        }

        if (authState.status == AuthFlowStatus.ERROR) {
            val panelLeft = width / 2 - 110
            val panelTop = minOf(height - 52, height / 4 + 146)
            val panelRight = width / 2 + 110
            val panelBottom = panelTop + 38

            context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC4A1A1A.toInt())
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Ошибка авторизации").styled { it.withColor(0xFF9A9A).withBold(true) },
                width / 2,
                panelTop + 6,
                0xFF9A9A.toInt(),
            )
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(authState.message),
                width / 2,
                panelTop + 18,
                0xFFEAEA.toInt(),
            )
        }
    }

    private fun statusLabel(status: AuthFlowStatus): String {
        return when (status) {
            AuthFlowStatus.IDLE -> "idle"
            AuthFlowStatus.OPENING_BROWSER -> "opening-browser"
            AuthFlowStatus.POLLING -> "polling"
            AuthFlowStatus.SUCCESS -> "success"
            AuthFlowStatus.ERROR -> "error"
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
        return "Auth server: " + AuthServerPresets.byId(workingConfig.selectedAuthServerId).label
    }
}
