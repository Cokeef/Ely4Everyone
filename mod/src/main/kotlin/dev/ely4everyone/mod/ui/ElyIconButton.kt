package dev.ely4everyone.mod.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.input.AbstractInput
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import kotlin.math.roundToInt

/**
 * Более заметная Ely.by-кнопка для title screen.
 *
 * Использует бренд-иконку и короткую fade/slide-анимацию при появлении.
 */
class ElyIconButton(
    x: Int,
    y: Int,
    private val onPressAction: () -> Unit,
) : PressableWidget(x, y, WIDTH, HEIGHT, BUTTON_LABEL) {
    private val createdAtMs = Util.getMeasuringTimeMs()

    init {
        setTooltip(Tooltip.of(BUTTON_TOOLTIP))
    }

    override fun drawIcon(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val revealProgress = ((Util.getMeasuringTimeMs() - createdAtMs).toFloat() / ANIMATION_DURATION_MS)
            .coerceIn(0f, 1f)
            .let(::easeOutCubic)
        val drawX = x - ((1f - revealProgress) * ANIMATION_OFFSET_X).roundToInt()
        val drawY = y + ((1f - revealProgress) * ANIMATION_OFFSET_Y).roundToInt()

        val topColor: Int
        val bottomColor: Int
        val borderColor: Int
        val labelColor: Int
        val accentColor: Int

        if (isSelected) {
            topColor = withAlpha(COLOR_TOP_HOVER, revealProgress)
            bottomColor = withAlpha(COLOR_BOTTOM_HOVER, revealProgress)
            borderColor = withAlpha(COLOR_BORDER_HOVER, revealProgress)
            labelColor = withAlpha(COLOR_TEXT_HOVER, revealProgress)
            accentColor = withAlpha(COLOR_ACCENT_HOVER, revealProgress)
        } else {
            topColor = withAlpha(COLOR_TOP, revealProgress)
            bottomColor = withAlpha(COLOR_BOTTOM, revealProgress)
            borderColor = withAlpha(COLOR_BORDER, revealProgress)
            labelColor = withAlpha(COLOR_TEXT, revealProgress)
            accentColor = withAlpha(COLOR_ACCENT, revealProgress)
        }

        context.fillGradient(drawX, drawY, drawX + width, drawY + height, topColor, bottomColor)
        context.fill(drawX, drawY, drawX + width, drawY + 1, borderColor)
        context.fill(drawX, drawY + height - 1, drawX + width, drawY + height, borderColor)
        context.fill(drawX, drawY + 1, drawX + 1, drawY + height - 1, borderColor)
        context.fill(drawX + width - 1, drawY + 1, drawX + width, drawY + height - 1, borderColor)

        val iconSlotRight = drawX + ICON_SLOT_WIDTH
        context.fillGradient(
            drawX + 1,
            drawY + 1,
            iconSlotRight,
            drawY + height - 1,
            accentColor,
            withAlpha(COLOR_ACCENT_BOTTOM, revealProgress),
        )
        context.fill(iconSlotRight, drawY + 3, iconSlotRight + 1, drawY + height - 3, borderColor)

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val iconX = drawX + 5
        val iconY = drawY + (height - ICON_SIZE) / 2
        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            ICON_TEXTURE,
            iconX,
            iconY,
            ICON_SIZE,
            ICON_SIZE,
            revealProgress,
        )

        val labelX = drawX + ICON_SLOT_WIDTH + 6
        val labelY = drawY + (height - textRenderer.fontHeight) / 2
        context.drawTextWithShadow(
            textRenderer,
            BUTTON_LABEL,
            labelX,
            labelY,
            labelColor,
        )
    }

    override fun onPress(input: AbstractInput) {
        onPressAction()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    companion object {
        const val WIDTH = 88
        const val HEIGHT = 20

        private const val ICON_SIZE = 14
        private const val ICON_SLOT_WIDTH = 24
        private const val ANIMATION_DURATION_MS = 420f
        private const val ANIMATION_OFFSET_X = 12f
        private const val ANIMATION_OFFSET_Y = 4f

        private val COLOR_TOP = 0xFF0C2A31.toInt()
        private val COLOR_BOTTOM = 0xFF143942.toInt()
        private val COLOR_BORDER = 0xFF2CA98B.toInt()
        private val COLOR_ACCENT = 0xFF164D4A.toInt()
        private val COLOR_ACCENT_BOTTOM = 0xFF0F3636.toInt()
        private val COLOR_TEXT = 0xFFE7FFF5.toInt()

        private val COLOR_TOP_HOVER = 0xFF123842.toInt()
        private val COLOR_BOTTOM_HOVER = 0xFF1A4951.toInt()
        private val COLOR_BORDER_HOVER = 0xFF54E4B0.toInt()
        private val COLOR_ACCENT_HOVER = 0xFF1F655F.toInt()
        private val COLOR_TEXT_HOVER = 0xFFFFFFFF.toInt()

        private val BUTTON_LABEL = Text.literal("Ely.by")
        private val BUTTON_TOOLTIP = Text.literal("Войти через Ely.by")
        private val ICON_TEXTURE = Identifier.of("ely4everyone", "textures/gui/ely_icon.png")

        private fun easeOutCubic(progress: Float): Float {
            val inverse = 1f - progress
            return 1f - inverse * inverse * inverse
        }

        private fun withAlpha(rgb: Int, alpha: Float): Int {
            val alphaChannel = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            return (alphaChannel shl 24) or (rgb and 0x00FFFFFF)
        }
    }
}
