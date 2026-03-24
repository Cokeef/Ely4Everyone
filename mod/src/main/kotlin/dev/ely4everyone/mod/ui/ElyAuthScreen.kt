package dev.ely4everyone.mod.ui

import dev.ely4everyone.mod.auth.AuthFlowState
import dev.ely4everyone.mod.auth.AuthFlowStatus
import dev.ely4everyone.mod.auth.AuthWorkflowManager
import dev.ely4everyone.mod.config.AuthServerPresets
import dev.ely4everyone.mod.config.ModConfig
import dev.ely4everyone.mod.config.ModConfigStore
import dev.ely4everyone.mod.session.ClientSessionState
import dev.ely4everyone.mod.session.ClientSessionStore
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.PlayerSkinWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.entity.player.PlayerSkinType
import net.minecraft.entity.player.SkinTextures
import net.minecraft.text.Text
import net.minecraft.util.AssetInfo
import net.minecraft.util.Identifier
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture

class ElyAuthScreen(
    private val parent: Screen,
) : Screen(Text.literal("Ely4Everyone")) {
    private var workingConfig: ModConfig = ModConfigStore.load()
    private var stage: ElyAuthStage = ElyAuthStage.HOST_SELECT
    private var forceHostSelection: Boolean = false

    private lateinit var authServerButton: ButtonWidget
    private lateinit var startLoginButton: ButtonWidget
    private lateinit var useLatestSessionButton: ButtonWidget
    private lateinit var hostBackButton: ButtonWidget
    private lateinit var waitingRetryButton: ButtonWidget
    private lateinit var waitingCancelButton: ButtonWidget
    private lateinit var successMenuButton: ButtonWidget
    private lateinit var successRetryButton: ButtonWidget
    private lateinit var successHostButton: ButtonWidget
    private lateinit var errorRetryButton: ButtonWidget
    private lateinit var errorMenuButton: ButtonWidget
    private lateinit var errorHostButton: ButtonWidget
    private lateinit var customAuthServerField: TextFieldWidget
    private lateinit var playerPreviewWidget: PlayerSkinWidget

    private var previewSourceKey: String? = null
    private var previewFuture: CompletableFuture<NativeImage?>? = null
    private var previewTexture: NativeImageBackedTexture? = null
    private var previewTextureId: Identifier? = null

    override fun init() {
        workingConfig = ModConfigStore.load()
        stage = determineStage()
        val layout = layout()

        customAuthServerField = TextFieldWidget(
            textRenderer,
            layout.left,
            layout.formTop + 66,
            layout.fieldWidth,
            20,
            Text.literal("Auth host URL"),
        ).apply {
            text = workingConfig.customAuthServerUrl
            setMaxLength(256)
        }
        addDrawableChild(customAuthServerField)

        authServerButton = addDrawableChild(
            ButtonWidget.builder(Text.literal(serverButtonLabel())) {
                cycleAuthServer()
            }.dimensions(layout.left, layout.formTop + 36, layout.fieldWidth, 20).build(),
        )

        startLoginButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Продолжить вход")) {
                saveWorkingConfig()
                forceHostSelection = false
                AuthWorkflowManager.startBrowserLogin()
                updateStage(ElyAuthStage.WAITING)
            }.dimensions(layout.left, layout.formTop + 100, layout.fieldWidth, 20).build(),
        )

        useLatestSessionButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Подтянуть последнюю сессию")) {
                saveWorkingConfig()
                forceHostSelection = false
                AuthWorkflowManager.syncLatestSessionFromAuthHost()
                updateStage(determineStage())
            }.dimensions(layout.left, layout.formTop + 126, layout.fieldWidth, 20).build(),
        )

        hostBackButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Назад в меню")) {
                AuthWorkflowManager.resetUiState()
                saveWorkingConfig()
                close()
            }.dimensions(layout.left, layout.formTop + 152, layout.fieldWidth, 20).build(),
        )

        waitingRetryButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Открыть браузер ещё раз")) {
                saveWorkingConfig()
                AuthWorkflowManager.startBrowserLogin()
            }.dimensions(layout.left, layout.formTop + 116, layout.fieldWidth, 20).build(),
        )

        waitingCancelButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Отменить")) {
                AuthWorkflowManager.cancelCurrentAttempt()
                forceHostSelection = true
                updateStage(ElyAuthStage.HOST_SELECT)
            }.dimensions(layout.left, layout.formTop + 142, layout.fieldWidth, 20).build(),
        )

        successMenuButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("В главное меню")) {
                AuthWorkflowManager.resetUiState("Локальная Ely-сессия готова.")
                close()
            }.dimensions(layout.left, layout.formTop + 150, 108, 20).build(),
        )

        successRetryButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Войти ещё раз")) {
                saveWorkingConfig()
                forceHostSelection = false
                AuthWorkflowManager.startBrowserLogin()
                updateStage(ElyAuthStage.WAITING)
            }.dimensions(layout.left + 112, layout.formTop + 150, 108, 20).build(),
        )

        successHostButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Сменить auth-host")) {
                forceHostSelection = true
                AuthWorkflowManager.resetUiState("Можно выбрать другой auth-host.")
                updateStage(ElyAuthStage.HOST_SELECT)
            }.dimensions(layout.left, layout.formTop + 176, layout.fieldWidth, 20).build(),
        )

        errorRetryButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Попробовать ещё раз")) {
                saveWorkingConfig()
                forceHostSelection = false
                AuthWorkflowManager.startBrowserLogin()
                updateStage(ElyAuthStage.WAITING)
            }.dimensions(layout.left, layout.formTop + 132, layout.fieldWidth, 20).build(),
        )

        errorMenuButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("В главное меню")) {
                AuthWorkflowManager.resetUiState()
                close()
            }.dimensions(layout.left, layout.formTop + 158, 108, 20).build(),
        )

        errorHostButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Сменить host")) {
                forceHostSelection = true
                AuthWorkflowManager.resetUiState("Выберите другой auth-host.")
                updateStage(ElyAuthStage.HOST_SELECT)
            }.dimensions(layout.left + 112, layout.formTop + 158, 108, 20).build(),
        )

        playerPreviewWidget = addDrawableChild(
            PlayerSkinWidget(
                72,
                90,
                client!!.loadedEntityModels,
                ::currentSkinTextures,
            ),
        )

        applyStageUi()
    }

    override fun tick() {
        pollPreviewTexture()
        val desiredStage = determineStage()
        if (desiredStage != stage) {
            updateStage(desiredStage)
        }
    }

    override fun close() {
        saveWorkingConfig()
        client?.setScreen(parent)
    }

    override fun removed() {
        clearPreviewTexture()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val layout = layout()
        val session = ClientSessionStore.load()
        val authState = AuthWorkflowManager.currentState()

        context.fillGradient(0, 0, width, height, 0xF010191C.toInt(), 0xF0181118.toInt())
        context.fill(0, 0, width, 30, 0xCC0A2B29.toInt())

        drawPanel(
            context,
            layout.panelLeft,
            layout.panelTop,
            layout.panelRight,
            layout.panelBottom,
            0xCC142126.toInt(),
            0xFF1F5B56.toInt(),
        )

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xF2FFF8.toInt())
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal(stage.subtitle),
            width / 2,
            32,
            0x86D9C4.toInt(),
        )

        renderStageHeader(context, layout, stage)

        when (stage) {
            ElyAuthStage.HOST_SELECT -> renderHostStage(context, layout, session)
            ElyAuthStage.WAITING -> renderWaitingStage(context, layout, authState)
            ElyAuthStage.SUCCESS -> renderSuccessStage(context, layout, session)
            ElyAuthStage.ERROR -> renderErrorStage(context, layout, authState)
        }

        if (customAuthServerField.visible) {
            customAuthServerField.render(context, mouseX, mouseY, delta)
        }

        listOf(
            authServerButton,
            startLoginButton,
            useLatestSessionButton,
            hostBackButton,
            waitingRetryButton,
            waitingCancelButton,
            successMenuButton,
            successRetryButton,
            successHostButton,
            errorRetryButton,
            errorMenuButton,
            errorHostButton,
            playerPreviewWidget,
        ).filter { it.visible }
            .forEach { it.render(context, mouseX, mouseY, delta) }
    }

    private fun renderStageHeader(context: DrawContext, layout: ElyAuthLayout, stage: ElyAuthStage) {
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(stage.title),
            layout.left,
            layout.panelTop + 12,
            0x7EE7C8.toInt(),
        )
    }

    private fun renderHostStage(context: DrawContext, layout: ElyAuthLayout, session: ClientSessionState) {
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Шаг 1. Выберите auth-host"),
            layout.left,
            layout.formTop + 8,
            0xF2FFF8.toInt(),
        )
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Сначала выбирается сервер авторизации, затем открывается браузер Ely.by."),
            layout.left,
            layout.formTop + 20,
            0x9AC4BC.toInt(),
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Источник авторизации"),
            layout.left,
            layout.formTop + 44,
            0x7EE7C8.toInt(),
        )
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Custom URL"),
            layout.left,
            layout.formTop + 74,
            if (workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM) 0xF2FFF8.toInt() else 0x6A8C84.toInt(),
        )

        val hintLines = mutableListOf(
            "Выбранный host: ${AuthServerPresets.byId(workingConfig.selectedAuthServerId).label}",
            "URL: ${workingConfig.resolvedAuthServerBaseUrl()}",
            "Ник клиента: ${currentClientUsername()}",
            "UUID клиента: ${currentClientUuid()}",
        )
        if (session.hasUsableAuthSession()) {
            hintLines += "Ely.by ник: ${session.elyUsername ?: "неизвестно"}"
            hintLines += "Ely.by UUID: ${session.elyUuid ?: "-"}"
        } else {
            hintLines += "Сохранённой Ely-сессии пока нет."
        }

        hintLines.forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                layout.left,
                layout.infoTop + index * 12,
                0xD8EFE7.toInt(),
            )
        }
    }

    private fun renderWaitingStage(context: DrawContext, layout: ElyAuthLayout, authState: AuthFlowState) {
        drawInfoCard(
            context,
            layout.left,
            layout.formTop + 44,
            layout.right,
            layout.formTop + 106,
            0xCC203342.toInt(),
            0xFF8ED7FF.toInt(),
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Шаг 2. Завершите вход в браузере"),
            layout.left + 10,
            layout.formTop + 52,
            0xFF8ED7FF.toInt(),
        )

        wrapText(
            "После успешного callback мод сам сохранит локальную Ely-сессию и покажет итоговый экран.",
            layout.fieldWidth - 20,
        ).forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                layout.left + 10,
                layout.formTop + 66 + index * 10,
                0xE3F6FF.toInt(),
            )
        }

        renderStatusStrip(context, layout, authState)
    }

    private fun renderSuccessStage(context: DrawContext, layout: ElyAuthLayout, session: ClientSessionState) {
        ensurePreviewRequested(session)

        val successLeft = width / 2 - 240
        val successRight = width / 2 + 240
        val summaryTop = layout.formTop + 40
        val summaryBottom = summaryTop + 116
        val detailsTop = summaryBottom + 8
        val detailsBottom = detailsTop + 64

        drawInfoCard(
            context,
            successLeft,
            summaryTop,
            successRight,
            summaryBottom,
            0xCC163926.toInt(),
            0xFF7CFF9A.toInt(),
        )

        drawInfoCard(
            context,
            successLeft,
            detailsTop,
            successRight,
            detailsBottom,
            0xCC12252A.toInt(),
            0xFF3B8C85.toInt(),
        )

        val previewLeft = successLeft + 14
        val previewTop = summaryTop + 12
        val previewSize = 92
        drawInfoCard(
            context,
            previewLeft,
            previewTop,
            previewLeft + previewSize,
            previewTop + previewSize,
            0xCC10272B.toInt(),
            0xFF5DD9C9.toInt(),
        )
        playerPreviewWidget.setX(previewLeft)
        playerPreviewWidget.setY(previewTop)
        playerPreviewWidget.setDimensions(previewSize, previewSize)

        val username = session.elyUsername ?: "неизвестно"
        val uuid = session.elyUuid ?: "-"
        val clientUsername = currentClientUsername()
        val activeSessionUuid = MinecraftClient.getInstance().session?.uuidOrNull?.toString() ?: "-"
        val uuidMatches = uuid == activeSessionUuid
        val sessionState = if (session.hasUsableAuthSession()) "Локальная Ely-сессия сохранена" else "Локальная Ely-сессия не активна"
        val infoLeft = previewLeft + previewSize + 18
        context.drawTextWithShadow(textRenderer, Text.literal("Авторизация успешна"), infoLeft, summaryTop + 10, 0xFF7CFF9A.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Ely.by ник: $username"), infoLeft, summaryTop + 28, 0xF2FFF8.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Ник клиента: $clientUsername"), infoLeft, summaryTop + 42, 0xF2FFF8.toInt())
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("UUID подменён: ${if (uuidMatches) "да" else "нет"}"),
            infoLeft,
            summaryTop + 56,
            if (uuidMatches) 0xC7FFD5.toInt() else 0xFFD0D0.toInt(),
        )
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Теперь можно играть с локально сохранённой Ely-сессией."),
            infoLeft,
            summaryTop + 76,
            0xBDEBD1.toInt(),
        )

        context.drawTextWithShadow(textRenderer, Text.literal("Ely UUID"), successLeft + 12, detailsTop + 10, 0x8FD9CE.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal(uuid), successLeft + 12, detailsTop + 24, 0xF2FFF8.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("UUID клиента"), successLeft + 12, detailsTop + 40, 0x8FD9CE.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal(activeSessionUuid), successLeft + 12, detailsTop + 54, 0xF2FFF8.toInt())
    }

    private fun renderErrorStage(context: DrawContext, layout: ElyAuthLayout, authState: AuthFlowState) {
        drawInfoCard(
            context,
            layout.left,
            layout.formTop + 44,
            layout.right,
            layout.formTop + 122,
            0xCC4A1E24.toInt(),
            0xFFFFA0A0.toInt(),
        )
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Авторизация не удалась"),
            layout.left + 10,
            layout.formTop + 52,
            0xFFFFA0A0.toInt(),
        )
        wrapText(authState.message, layout.fieldWidth - 20).take(4).forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                layout.left + 10,
                layout.formTop + 66 + index * 10,
                0xFFE6E6.toInt(),
            )
        }
    }

    private fun renderStatusStrip(context: DrawContext, layout: ElyAuthLayout, authState: AuthFlowState) {
        val fillColor = when (authState.status) {
            AuthFlowStatus.SUCCESS -> 0xCC163926.toInt()
            AuthFlowStatus.ERROR -> 0xCC4A1E24.toInt()
            AuthFlowStatus.POLLING, AuthFlowStatus.OPENING_BROWSER -> 0xCC203342.toInt()
            AuthFlowStatus.IDLE -> 0xCC1E2830.toInt()
        }
        val accentColor = when (authState.status) {
            AuthFlowStatus.SUCCESS -> 0xFF7CFF9A.toInt()
            AuthFlowStatus.ERROR -> 0xFFFFA0A0.toInt()
            AuthFlowStatus.POLLING, AuthFlowStatus.OPENING_BROWSER -> 0xFF8ED7FF.toInt()
            AuthFlowStatus.IDLE -> 0xFFD7E6EE.toInt()
        }

        drawInfoCard(context, layout.left, layout.infoTop, layout.right, layout.infoTop + 42, fillColor, accentColor)
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Статус: ${statusLabel(authState.status)}"),
            layout.left + 10,
            layout.infoTop + 8,
            accentColor,
        )
        wrapText(authState.message, layout.fieldWidth - 20).take(2).forEachIndexed { index, line ->
            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                layout.left + 10,
                layout.infoTop + 20 + index * 10,
                0xE3F6FF.toInt(),
            )
        }
    }

    private fun currentSkinTextures(): SkinTextures {
        val session = ClientSessionStore.load()
        val textureId = previewTextureId
        if (textureId != null) {
            return SkinTextures.create(
                object : AssetInfo.TextureAsset {
                    override fun id(): Identifier = textureId

                    override fun texturePath(): Identifier = textureId
                },
                null,
                null,
                extractSkinModel(session.elyTexturesValue),
            )
        }

        val uuid = session.elyUuid
            ?.let { rawUuid -> runCatching { UUID.fromString(rawUuid) }.getOrNull() }
            ?: MinecraftClient.getInstance().session?.uuidOrNull
            ?: UUID.randomUUID()
        return DefaultSkinHelper.getSkinTextures(uuid)
    }

    private fun drawPanel(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, fillColor: Int, borderColor: Int) {
        context.fill(left, top, right, bottom, fillColor)
        context.fill(left, top, right, top + 1, borderColor)
        context.fill(left, bottom - 1, right, bottom, borderColor)
        context.fill(left, top, left + 1, bottom, borderColor)
        context.fill(right - 1, top, right, bottom, borderColor)
    }

    private fun drawInfoCard(context: DrawContext, left: Int, top: Int, right: Int, bottom: Int, fillColor: Int, borderColor: Int) {
        drawPanel(context, left, top, right, bottom, fillColor, borderColor)
    }

    private fun determineStage(): ElyAuthStage {
        val authState = AuthWorkflowManager.currentState()
        val session = ClientSessionStore.load()
        return when {
            authState.status == AuthFlowStatus.OPENING_BROWSER || authState.status == AuthFlowStatus.POLLING -> ElyAuthStage.WAITING
            forceHostSelection -> ElyAuthStage.HOST_SELECT
            authState.status == AuthFlowStatus.ERROR -> ElyAuthStage.ERROR
            authState.status == AuthFlowStatus.SUCCESS || session.hasUsableAuthSession() -> ElyAuthStage.SUCCESS
            else -> ElyAuthStage.HOST_SELECT
        }
    }

    private fun updateStage(newStage: ElyAuthStage) {
        stage = newStage
        applyStageUi()
    }

    private fun applyStageUi() {
        val showHostSelect = stage == ElyAuthStage.HOST_SELECT
        val showWaiting = stage == ElyAuthStage.WAITING
        val showSuccess = stage == ElyAuthStage.SUCCESS
        val showError = stage == ElyAuthStage.ERROR

        authServerButton.visible = showHostSelect
        authServerButton.active = showHostSelect
        customAuthServerField.visible = showHostSelect
        customAuthServerField.active = showHostSelect
        customAuthServerField.setEditable(showHostSelect && workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM)
        startLoginButton.visible = showHostSelect
        startLoginButton.active = showHostSelect
        useLatestSessionButton.visible = showHostSelect
        useLatestSessionButton.active = showHostSelect
        hostBackButton.visible = showHostSelect
        hostBackButton.active = showHostSelect

        waitingRetryButton.visible = showWaiting
        waitingRetryButton.active = showWaiting
        waitingCancelButton.visible = showWaiting
        waitingCancelButton.active = showWaiting

        successMenuButton.visible = showSuccess
        successMenuButton.active = showSuccess
        successRetryButton.visible = showSuccess
        successRetryButton.active = showSuccess
        successHostButton.visible = showSuccess
        successHostButton.active = showSuccess
        playerPreviewWidget.visible = showSuccess
        playerPreviewWidget.active = showSuccess
        if (showSuccess) {
            val successLeft = width / 2 - 240
            val successRight = width / 2 + 240
            val buttonTop = layout().formTop + 230
            successMenuButton.setX(successLeft)
            successMenuButton.setY(buttonTop)
            successRetryButton.setX(successLeft + 244)
            successRetryButton.setY(buttonTop)
            successHostButton.setX(successLeft)
            successHostButton.setY(buttonTop + 26)
            successHostButton.width = successRight - successLeft
            playerPreviewWidget.setX(successLeft + 14)
            playerPreviewWidget.setY(layout().formTop + 52)
            playerPreviewWidget.setDimensions(92, 92)
        }

        errorRetryButton.visible = showError
        errorRetryButton.active = showError
        errorMenuButton.visible = showError
        errorMenuButton.active = showError
        errorHostButton.visible = showError
        errorHostButton.active = showError
    }

    private fun cycleAuthServer() {
        val currentIndex = AuthServerPresets.ALL.indexOfFirst { it.id == workingConfig.selectedAuthServerId }
        val nextPreset = AuthServerPresets.ALL[(currentIndex + 1).mod(AuthServerPresets.ALL.size)]
        workingConfig = workingConfig.copy(
            selectedAuthServerId = nextPreset.id,
            relayBaseUrl = nextPreset.defaultUrl ?: customAuthServerField.text.ifBlank { workingConfig.relayBaseUrl },
        )
        customAuthServerField.setEditable(workingConfig.selectedAuthServerId == AuthServerPresets.CUSTOM)
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

    private fun currentClientUsername(): String {
        return MinecraftClient.getInstance().session?.username ?: "неизвестно"
    }

    private fun currentClientUuid(): String {
        return MinecraftClient.getInstance().session?.uuidOrNull?.toString() ?: "-"
    }

    private fun statusLabel(status: AuthFlowStatus): String {
        return when (status) {
            AuthFlowStatus.IDLE -> "Готов"
            AuthFlowStatus.OPENING_BROWSER -> "Открываю браузер"
            AuthFlowStatus.POLLING -> "Ожидание callback-а"
            AuthFlowStatus.SUCCESS -> "Успешно"
            AuthFlowStatus.ERROR -> "Ошибка"
        }
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

    private fun layout(): ElyAuthLayout {
        val fieldWidth = 220
        val left = width / 2 - fieldWidth / 2
        val panelTop = 40
        return ElyAuthLayout(
            left = left,
            right = left + fieldWidth,
            fieldWidth = fieldWidth,
            panelLeft = left - 20,
            panelRight = left + fieldWidth + 20,
            panelTop = panelTop,
            panelBottom = minOf(height - 18, panelTop + 252),
            formTop = panelTop + 18,
            infoTop = panelTop + 202,
        )
    }

    private fun ensurePreviewRequested(session: ClientSessionState) {
        val texturesValue = session.elyTexturesValue ?: return
        if (texturesValue == previewSourceKey || previewFuture != null || previewTextureId != null) {
            return
        }

        previewSourceKey = texturesValue
        val skinUrl = extractSkinUrl(texturesValue) ?: return
        previewFuture = CompletableFuture.supplyAsync {
            runCatching {
                URI.create(skinUrl).toURL().openStream().use(NativeImage::read)
            }.getOrNull()
        }
    }

    private fun pollPreviewTexture() {
        val future = previewFuture ?: return
        if (!future.isDone) {
            return
        }

        previewFuture = null
        val image = runCatching { future.get() }.getOrNull() ?: return
        val texture = NativeImageBackedTexture({ "ely-auth-preview" }, image)
        val textureId = Identifier.of("ely4everyone", "preview/auth_skin")
        client?.textureManager?.registerTexture(textureId, texture)
        previewTexture?.close()
        previewTexture = texture
        previewTextureId = textureId
    }

    private fun clearPreviewTexture() {
        previewFuture = null
        previewSourceKey = null
        previewTexture?.close()
        previewTexture = null
        previewTextureId = null
    }

    private fun extractSkinModel(texturesValue: String?): PlayerSkinType {
        val decoded = texturesValue?.let {
            runCatching { String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) }.getOrNull()
        } ?: return PlayerSkinType.WIDE
        val metadata = Regex("\"metadata\"\\s*:\\s*\\{[^}]*\"model\"\\s*:\\s*\"([^\"]+)\"")
            .find(decoded)
            ?.groupValues
            ?.getOrNull(1)
        return PlayerSkinType.byModelMetadata(metadata)
    }

    private fun extractSkinUrl(texturesValue: String): String? {
        return runCatching {
            val decoded = String(Base64.getDecoder().decode(texturesValue), StandardCharsets.UTF_8)
            Regex(""""SKIN"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")
                .find(decoded)
                ?.groupValues
                ?.getOrNull(1)
        }.getOrNull()
    }
}

private enum class ElyAuthStage(
    val title: String,
    val subtitle: String,
) {
    HOST_SELECT(
        title = "Выбор auth-host",
        subtitle = "Сначала выберите, где будет проходить авторизация Ely.by",
    ),
    WAITING(
        title = "Ожидание callback-а",
        subtitle = "Браузер уже открыт, мод ждёт ответ от Ely.by и auth-host",
    ),
    SUCCESS(
        title = "Авторизация завершена",
        subtitle = "Ely-сессия сохранена локально и готова к использованию",
    ),
    ERROR(
        title = "Ошибка авторизации",
        subtitle = "Можно попробовать снова или выбрать другой auth-host",
    ),
}

private data class ElyAuthLayout(
    val left: Int,
    val right: Int,
    val fieldWidth: Int,
    val panelLeft: Int,
    val panelRight: Int,
    val panelTop: Int,
    val panelBottom: Int,
    val formTop: Int,
    val infoTop: Int,
)
