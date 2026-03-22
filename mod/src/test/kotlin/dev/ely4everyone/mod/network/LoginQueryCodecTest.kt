package dev.ely4everyone.mod.network

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginQueryCodecTest {
    @Test
    fun `decodes challenge and encodes response`() {
        val challengeBuf = PacketByteBufs.create().apply {
            writeBytes(
                """
                ver=v1
                nonce=test-nonce
                aud=test-audience
                """.trimIndent().toByteArray(),
            )
        }

        val challenge = LoginQueryCodec.decodeChallenge(challengeBuf)
        assertEquals("v1", challenge.version)
        assertEquals("test-nonce", challenge.nonce)
        assertEquals("test-audience", challenge.audience)

        val responseBuf = LoginQueryCodec.encodeResponse("ticket-123")
        val payload = ByteArray(responseBuf.readableBytes())
        responseBuf.readBytes(payload)
        assertEquals("ver=v1\nticket=ticket-123", payload.toString(Charsets.UTF_8))
    }
}
