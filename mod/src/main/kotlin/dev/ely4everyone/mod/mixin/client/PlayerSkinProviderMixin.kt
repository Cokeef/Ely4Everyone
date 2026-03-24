package dev.ely4everyone.mod.mixin.client

import com.mojang.authlib.GameProfile
import dev.ely4everyone.mod.identity.ActiveElyIdentityManager
import net.minecraft.client.texture.PlayerSkinProvider
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.entity.player.SkinTextures
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.Optional
import java.util.concurrent.CompletableFuture

@Mixin(PlayerSkinProvider::class)
abstract class PlayerSkinProviderMixin {
    @Inject(
        method = ["fetchSkinTextures(Lcom/mojang/authlib/GameProfile;)Ljava/util/concurrent/CompletableFuture;"],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun `ely4everyone$fetchSkinTexturesForActiveIdentity`(
        profile: GameProfile,
        cir: CallbackInfoReturnable<CompletableFuture<Optional<SkinTextures>>>,
    ) {
        val activeState = ActiveElyIdentityManager.activeStateOrNull() ?: return
        if (activeState.gameProfile.id != profile.id || activeState.gameProfile.name != profile.name) {
            return
        }

        val profileTextures = activeState.profileTextures
        if (profileTextures != null) {
            val provider = this as PlayerSkinProviderInvoker
            cir.returnValue = provider.`ely4everyone$invokeFetchSkinTextures`(activeState.elySession.uuidOrNull, profileTextures)
                .thenApply { skinTextures -> Optional.of(skinTextures) }
            return
        }

        cir.returnValue = CompletableFuture.completedFuture(Optional.of(DefaultSkinHelper.getSkinTextures(activeState.gameProfile)))
    }
}
