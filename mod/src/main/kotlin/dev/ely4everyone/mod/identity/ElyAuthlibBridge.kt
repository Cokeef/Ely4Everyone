package dev.ely4everyone.mod.identity

import com.mojang.authlib.Environment
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.exceptions.ForcedUsernameChangeException
import com.mojang.authlib.exceptions.InsufficientPrivilegesException
import com.mojang.authlib.exceptions.InvalidCredentialsException
import com.mojang.authlib.exceptions.UserBannedException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import dev.ely4everyone.mod.Ely4EveryoneClientMod
import dev.ely4everyone.mod.session.ClientSessionStore
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.net.Proxy

object ElyAuthlibBridge {
    private val logger = LoggerFactory.getLogger("${Ely4EveryoneClientMod.MOD_ID}/authlib")
    private val elyEnvironment = Environment(
        "https://authserver.ely.by",
        "https://authserver.ely.by",
        "https://authserver.ely.by",
        "ELY",
    )

    fun hasUsableElySession(): Boolean {
        return ClientSessionStore.load().hasUsableElyAccessToken()
    }

    fun tryJoinServerSession(serverId: String): Text? {
        val client = MinecraftClient.getInstance()
        val session = client.session ?: return null
        val uuid = session.uuidOrNull ?: return null
        val accessToken = session.accessToken

        if (accessToken.isBlank()) {
            return Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.invalidSession"),
            )
        }

        val authService = YggdrasilAuthenticationService(Proxy.NO_PROXY, elyEnvironment)
        val sessionService = authService.createMinecraftSessionService()

        return try {
            logger.info("Joining server session through Ely authserver. uuid={}, serverId={}", uuid, serverId)
            sessionService.joinServer(uuid, accessToken, serverId)
            null
        } catch (_: AuthenticationUnavailableException) {
            Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.serversUnavailable"),
            )
        } catch (_: InvalidCredentialsException) {
            Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.invalidSession"),
            )
        } catch (_: InsufficientPrivilegesException) {
            Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.insufficientPrivileges"),
            )
        } catch (_: UserBannedException) {
            Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.userBanned"),
            )
        } catch (_: ForcedUsernameChangeException) {
            Text.translatable(
                "disconnect.loginFailedInfo",
                Text.translatable("disconnect.loginFailedInfo.userBanned"),
            )
        } catch (exception: AuthenticationException) {
            logger.warn("Ely joinServerSession failed.", exception)
            Text.translatable("disconnect.loginFailedInfo", exception.message ?: "Authentication error")
        }
    }
}

