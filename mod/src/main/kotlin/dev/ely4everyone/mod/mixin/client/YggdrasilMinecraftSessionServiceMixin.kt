package dev.ely4everyone.mod.mixin.client

import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService
import dev.ely4everyone.mod.identity.ElyIdentityManager
import dev.ely4everyone.mod.session.ClientSessionStore
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(YggdrasilMinecraftSessionService::class)
abstract class YggdrasilMinecraftSessionServiceMixin {
    @Inject(method = ["getPackedTextures"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$provideTexturesForActiveIdentity`(
        profile: GameProfile,
        cir: CallbackInfoReturnable<Property?>,
    ) {
        val identity = ElyIdentityManager.fromClientSession(ClientSessionStore.load()) ?: return
        val currentProfile = ElyIdentityManager.toGameProfile(identity) ?: return
        if (currentProfile.id != profile.id || currentProfile.name != profile.name) {
            return
        }

        cir.returnValue = ElyIdentityManager.toTexturesProperty(identity)
    }

    @Inject(method = ["unpackTextures"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$unpackTexturesForActiveIdentity`(
        property: Property,
        cir: CallbackInfoReturnable<MinecraftProfileTextures>,
    ) {
        val identity = ElyIdentityManager.fromClientSession(ClientSessionStore.load()) ?: return
        val currentTextures = ElyIdentityManager.toTexturesProperty(identity) ?: return
        if (currentTextures.value != property.value || currentTextures.signature != property.signature) {
            return
        }

        cir.returnValue = ElyIdentityManager.toMinecraftProfileTextures(identity)
    }
}
