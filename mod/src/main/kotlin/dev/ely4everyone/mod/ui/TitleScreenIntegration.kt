package dev.ely4everyone.mod.ui

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text

object TitleScreenIntegration {
    fun register() {
        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            if (screen !is TitleScreen) {
                return@register
            }

            Screens.getButtons(screen).add(
                ButtonWidget.builder(
                    Text.literal("E").styled { style ->
                        style.withColor(0x3DDC84).withBold(true)
                    },
                ) {
                    client.setScreen(ElyAuthScreen(screen))
                }
                    .dimensions(screen.width / 2 - 148, screen.height / 4 + 48 + 84, 20, 20)
                    .tooltip(Tooltip.of(Text.literal("Ely4Everyone / Ely.by auth")))
                    .build(),
            )
        }
    }
}
