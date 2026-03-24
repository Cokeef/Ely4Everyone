package dev.ely4everyone.mod.mixin.client

import dev.ely4everyone.mod.identity.ElyIdentityManager
import dev.ely4everyone.mod.session.ClientSessionStore
import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import com.mojang.authlib.GameProfile

@Mixin(MinecraftClient::class)
abstract class MinecraftClientGameProfileMixin {
    @Inject(method = ["getGameProfile"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$overrideGameProfile`(cir: CallbackInfoReturnable<GameProfile>) {
        val sessionState = ClientSessionStore.load()
        val identity = ElyIdentityManager.fromClientSession(sessionState) ?: return
        val profile = ElyIdentityManager.toGameProfile(identity) ?: return
        cir.returnValue = profile
    }
}
