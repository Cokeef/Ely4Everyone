package dev.ely4everyone.mod.mixin.client

import com.mojang.authlib.GameProfile
import dev.ely4everyone.mod.identity.ActiveElyIdentityManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.session.ProfileKeys
import net.minecraft.client.session.Session
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(MinecraftClient::class)
abstract class MinecraftClientGameProfileMixin {
    @Inject(method = ["getSession"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$overrideSession`(cir: CallbackInfoReturnable<Session>) {
        ActiveElyIdentityManager.currentSessionOrNull()?.let { cir.returnValue = it }
    }

    @Inject(method = ["getGameProfile"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$overrideGameProfile`(cir: CallbackInfoReturnable<GameProfile>) {
        ActiveElyIdentityManager.currentGameProfileOrNull()?.let { cir.returnValue = it }
    }

    @Inject(method = ["getProfileKeys"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$overrideProfileKeys`(cir: CallbackInfoReturnable<ProfileKeys>) {
        if (ActiveElyIdentityManager.isActive()) {
            cir.returnValue = ProfileKeys.MISSING
        }
    }
}
