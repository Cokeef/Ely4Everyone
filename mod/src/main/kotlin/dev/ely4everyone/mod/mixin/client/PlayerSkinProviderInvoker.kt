package dev.ely4everyone.mod.mixin.client

import com.mojang.authlib.minecraft.MinecraftProfileTextures
import net.minecraft.client.texture.PlayerSkinProvider
import net.minecraft.entity.player.SkinTextures
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Mixin(PlayerSkinProvider::class)
interface PlayerSkinProviderInvoker {
    @Invoker("fetchSkinTextures")
    fun `ely4everyone$invokeFetchSkinTextures`(
        uuid: UUID,
        profileTextures: MinecraftProfileTextures,
    ): CompletableFuture<SkinTextures>
}
