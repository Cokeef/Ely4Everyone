package dev.ely4everyone.mod.ui

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.screen.TitleScreen

object TitleScreenIntegration {
    fun register() {
        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            if (screen !is TitleScreen) {
                return@register
            }

            Screens.getButtons(screen).add(
                ElyIconButton(
                    x = screen.width / 2 - 100 - ElyIconButton.WIDTH - 4,
                    y = screen.height / 4 + 48 + 84,
                ) {
                    client.setScreen(ElyAuthScreen(screen))
                },
            )
        }
    }
}
