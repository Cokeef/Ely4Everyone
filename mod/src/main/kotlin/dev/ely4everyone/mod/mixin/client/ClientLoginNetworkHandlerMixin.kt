package dev.ely4everyone.mod.mixin.client

import dev.ely4everyone.mod.identity.ElyAuthlibBridge
import net.minecraft.client.network.ClientLoginNetworkHandler
import net.minecraft.text.Text
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ClientLoginNetworkHandler::class)
abstract class ClientLoginNetworkHandlerMixin {
    @Inject(method = ["joinServerSession"], at = [At("HEAD")], cancellable = true)
    private fun `ely4everyone$joinViaEly`(serverId: String, cir: CallbackInfoReturnable<Text?>) {
        if (!ElyAuthlibBridge.hasUsableElySession()) {
            return
        }

        cir.returnValue = ElyAuthlibBridge.tryJoinServerSession(serverId)
    }
}
