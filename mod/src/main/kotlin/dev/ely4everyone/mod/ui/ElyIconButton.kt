package dev.ely4everyone.mod.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.input.AbstractInput
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * Квадратная Ely.by-кнопка для title screen.
 */
class ElyIconButton(
    x: Int,
    y: Int,
    private val onPressAction: () -> Unit,
) : PressableWidget(x, y, SIZE, SIZE, BUTTON_LABEL) {

    init {
        setTooltip(Tooltip.of(BUTTON_TOOLTIP))
    }

    override fun drawIcon(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val drawX = x
        val drawY = y
        context.fill(drawX - 1, drawY - 1, drawX + width + 1, drawY + height + 1, COLOR_SHADOW)

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
            context.fill(drawX - 1, drawY - 1, drawX + width + 1, drawY, COLOR_BORDER_HOVER)
            context.fill(drawX - 1, drawY + height, drawX + width + 1, drawY + height + 1, COLOR_BORDER_HOVER)
            context.fill(drawX - 1, drawY, drawX, drawY + height, COLOR_BORDER_HOVER)
            context.fill(drawX + width, drawY, drawX + width + 1, drawY + height, COLOR_BORDER_HOVER)
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

        private val COLOR_BORDER_HOVER = 0xFF54E4B0.toInt()
        private val COLOR_SHADOW = 0x8C041316.toInt()

        private val BUTTON_LABEL = Text.literal("Ely.by auth")
        private val BUTTON_TOOLTIP = Text.literal("Войти через Ely.by")
        private val ICON_TEXTURE = Identifier.of("ely4everyone", "textures/gui/ely_icon.png")
    }
}
