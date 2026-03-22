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
 * Квадратная Ely.by-кнопка для title screen.
 *
 * Использует бренд-иконку и короткую slide-анимацию при появлении.
 */
class ElyIconButton(
    x: Int,
    y: Int,
    private val onPressAction: () -> Unit,
) : PressableWidget(x, y, SIZE, SIZE, BUTTON_LABEL) {
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
        val shadowColor = withAlpha(COLOR_SHADOW, 0.55f * revealProgress)
        context.fill(drawX - 1, drawY - 1, drawX + width + 1, drawY + height + 1, shadowColor)

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            ICON_TEXTURE,
            drawX,
            drawY,
            0f,
            0f,
            width,
            height,
            width,
            height,
        )

        if (isSelected) {
            val borderColor = withAlpha(COLOR_BORDER_HOVER, revealProgress)
            context.fill(drawX - 1, drawY - 1, drawX + width + 1, drawY, borderColor)
            context.fill(drawX - 1, drawY + height, drawX + width + 1, drawY + height + 1, borderColor)
            context.fill(drawX - 1, drawY, drawX, drawY + height, borderColor)
            context.fill(drawX + width, drawY, drawX + width + 1, drawY + height, borderColor)
        }
    }

    override fun onPress(input: AbstractInput) {
        onPressAction()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    companion object {
        const val SIZE = 20

        private const val ANIMATION_DURATION_MS = 420f
        private const val ANIMATION_OFFSET_X = 8f
        private const val ANIMATION_OFFSET_Y = 3f

        private val COLOR_BORDER_HOVER = 0xFF54E4B0.toInt()
        private val COLOR_SHADOW = 0xFF041316.toInt()

        private val BUTTON_LABEL = Text.literal("Ely.by auth")
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
