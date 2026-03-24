package dev.ely4everyone.mod.identity

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.SignatureState
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import dev.ely4everyone.mod.session.ClientSessionState
import java.time.Instant
import java.util.Base64
import java.util.HashMap
import java.util.UUID

object ElyIdentityManager {
    fun fromClientSession(sessionState: ClientSessionState, now: Instant = Instant.now()): ElyIdentity? {
        if (!sessionState.hasRestorableElyIdentity(now)) {
            return null
        }

        return ElyIdentity(
            username = sessionState.elyUsername.orEmpty().trim(),
            uuid = sessionState.elyUuid.orEmpty().trim(),
            accessToken = sessionState.elyAccessToken.orEmpty().trim(),
            texturesValue = normalizeTexturesValue(sessionState.elyTexturesValue),
            texturesSignature = sessionState.elyTexturesSignature,
        )
    }

    fun toGameProfile(identity: ElyIdentity): GameProfile? {
        val uuid = runCatching { UUID.fromString(identity.uuid) }.getOrNull() ?: return null
        val builder = ImmutableMultimap.builder<String, Property>()
        toTexturesProperty(identity)?.let { texturesProperty ->
            builder.put(texturesProperty.name, texturesProperty)
        }
        val properties = PropertyMap(builder.build())
        return GameProfile(uuid, identity.username, properties)
    }

    fun toTexturesProperty(identity: ElyIdentity): Property? {
        val texturesValue = identity.texturesValue?.takeIf { it.isNotBlank() } ?: return null
        return identity.texturesSignature
            ?.takeIf { it.isNotBlank() }
            ?.let { Property("textures", texturesValue, it) }
            ?: Property("textures", texturesValue)
    }

    fun toMinecraftProfileTextures(identity: ElyIdentity): MinecraftProfileTextures? {
        val texturesValue = identity.texturesValue?.takeIf { it.isNotBlank() } ?: return null
        val decoded = runCatching {
            String(Base64.getDecoder().decode(texturesValue), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val skinUrl = Regex(""""SKIN"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"""")
            .find(decoded)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null

        val metadata = HashMap<String, String>()
        Regex(""""metadata"\s*:\s*\{[^}]*"model"\s*:\s*"([^"]+)"""")
            .find(decoded)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { metadata["model"] = it }

        return MinecraftProfileTextures(
            MinecraftProfileTexture(skinUrl, metadata),
            null,
            null,
            if (identity.texturesSignature.isNullOrBlank()) SignatureState.UNSIGNED else SignatureState.SIGNED,
        )
    }

    private fun normalizeTexturesValue(value: String?): String? {
        if (value.isNullOrBlank()) {
            return value
        }

        return runCatching {
            val decoded = String(java.util.Base64.getDecoder().decode(value), Charsets.UTF_8)
            val normalized = decoded.replace("\"url\":\"http://ely.by/", "\"url\":\"https://ely.by/")
            if (normalized == decoded) {
                value
            } else {
                java.util.Base64.getEncoder().encodeToString(normalized.toByteArray(Charsets.UTF_8))
            }
        }.getOrDefault(value)
    }
}
