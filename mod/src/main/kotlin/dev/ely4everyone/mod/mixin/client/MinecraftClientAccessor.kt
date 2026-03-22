package dev.ely4everyone.mod.mixin.client

import net.minecraft.client.MinecraftClient
import net.minecraft.client.session.Session
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(MinecraftClient::class)
interface MinecraftClientAccessor {
    @Mutable
    @Accessor("session")
    fun `ely4everyone$setSession`(session: Session)
}
