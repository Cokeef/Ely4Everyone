package dev.ely4everyone.mod.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.input.AbstractInput
import net.minecraft.text.Text

/**
 * Кнопка с иконкой "E" в стиле Ely.by для title screen.
 *
 * Рендерит градиентный фон (бирюзовый -> зелёный) с белой "E" по центру.
 * При hover/focus фон и обводка становятся ярче.
 */
class ElyIconButton(
    x: Int,
    y: Int,
    private val onPressAction: () -> Unit,
) : PressableWidget(x, y, SIZE, SIZE, BUTTON_TEXT) {

    init {
        setTooltip(Tooltip.of(BUTTON_TEXT))
    }

    override fun drawIcon(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val topColor: Int
        val bottomColor: Int
        val borderColor: Int

        if (isSelected) {
            topColor = COLOR_TOP_HOVER
            bottomColor = COLOR_BOTTOM_HOVER
            borderColor = COLOR_BORDER_HOVER
        } else {
            topColor = COLOR_TOP
            bottomColor = COLOR_BOTTOM
            borderColor = COLOR_BORDER
        }

        // Gradient background (Ely.by brand: teal → green)
        context.fillGradient(x, y, x + width, y + height, topColor, bottomColor)

        // 1px border
        context.fill(x, y, x + width, y + 1, borderColor)
        context.fill(x, y + height - 1, x + width, y + height, borderColor)
        context.fill(x, y + 1, x + 1, y + height - 1, borderColor)
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor)

        // "E" letter, bold, white, centered
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textWidth = textRenderer.getWidth(ICON_TEXT)
        context.drawTextWithShadow(
            textRenderer,
            ICON_TEXT,
            x + (width - textWidth) / 2,
            y + (height - textRenderer.fontHeight) / 2,
            COLOR_TEXT,
        )
    }

    override fun onPress(input: AbstractInput) {
        onPressAction()
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    companion object {
        const val SIZE = 20

        // Ely.by brand palette
        private val COLOR_TOP = 0xFF2BA4B3.toInt()
        private val COLOR_BOTTOM = 0xFF3DDC84.toInt()
        private val COLOR_BORDER = 0xFF209088.toInt()

        // Brighter on hover
        private val COLOR_TOP_HOVER = 0xFF38C0D0.toInt()
        private val COLOR_BOTTOM_HOVER = 0xFF50EE9A.toInt()
        private val COLOR_BORDER_HOVER = 0xFF5AEFB0.toInt()

        private const val COLOR_TEXT = 0xFFFFFF

        private val BUTTON_TEXT = Text.literal("Ely4Everyone / Ely.by auth")
        private val ICON_TEXT = Text.literal("E").styled { it.withBold(true) }
    }
}
