package dev.ely4everyone.mod.identity

import com.mojang.authlib.properties.Property
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ElyGameProfileMappingTest {
    @Test
    fun `maps Ely identity to game profile with Ely uuid and username`() {
        val identity = ElyIdentity(
            username = "Cokeef",
            uuid = "123e4567-e89b-12d3-a456-426614174000",
            accessToken = "ely-access",
            texturesValue = "textures-value",
            texturesSignature = "textures-signature",
        )

        val profile = ElyIdentityManager.toGameProfile(identity)

        assertNotNull(profile)
        assertEquals("Cokeef", profile.name)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", profile.id.toString())
    }

    @Test
    fun `maps Ely textures into signed property`() {
        val identity = ElyIdentity(
            username = "Cokeef",
            uuid = "123e4567-e89b-12d3-a456-426614174000",
            accessToken = "ely-access",
            texturesValue = "textures-value",
            texturesSignature = "textures-signature",
        )

        val textures = ElyIdentityManager.toTexturesProperty(identity)

        assertEquals(Property("textures", "textures-value", "textures-signature"), textures)
    }

    @Test
    fun `returns null textures property when Ely textures are absent`() {
        val identity = ElyIdentity(
            username = "Cokeef",
            uuid = "123e4567-e89b-12d3-a456-426614174000",
            accessToken = "ely-access",
        )

        assertNull(ElyIdentityManager.toTexturesProperty(identity))
    }

    @Test
    fun `returns null for invalid Ely uuid`() {
        val identity = ElyIdentity(
            username = "Cokeef",
            uuid = "not-a-uuid",
            accessToken = "ely-access",
            texturesValue = "textures-value",
        )

        assertNull(ElyIdentityManager.toGameProfile(identity))
    }

    @Test
    fun `builds minecraft profile textures from Ely textures payload`() {
        val payload = """{"timestamp":1,"profileId":"123","profileName":"Cokeef","textures":{"SKIN":{"url":"https://ely.by/storage/skins/example.png","metadata":{"model":"slim"}}}}"""
        val identity = ElyIdentity(
            username = "Cokeef",
            uuid = "123e4567-e89b-12d3-a456-426614174000",
            accessToken = "ely-access",
            texturesValue = java.util.Base64.getEncoder().encodeToString(payload.toByteArray()),
            texturesSignature = null,
        )

        val textures = ElyIdentityManager.toMinecraftProfileTextures(identity)

        assertNotNull(textures)
        assertEquals("https://ely.by/storage/skins/example.png", textures.skin()?.url)
        assertEquals("slim", textures.skin()?.getMetadata("model"))
        assertTrue(textures.signatureState().name == "UNSIGNED")
    }
}
