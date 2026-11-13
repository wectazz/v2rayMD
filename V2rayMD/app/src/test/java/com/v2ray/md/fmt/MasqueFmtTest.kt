package com.v2ray.md.fmt

import com.v2ray.md.AppConfig
import com.v2ray.md.enums.NetworkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MasqueFmtTest {

    @Test
    fun parseFillsMasqueDefaults() {
        val profile = MasqueFmt.parse(
            "masque://secret@example.com:443?publickey=pub&address=172.16.0.2&dns=1.1.1.1&headers=%7B%22a%22%3A%22b%22%7D"
        )

        assertEquals("example.com", profile?.server)
        assertEquals("443", profile?.serverPort)
        assertEquals("secret", profile?.secretKey)
        assertEquals("pub", profile?.publicKey)
        assertEquals(AppConfig.TLS, profile?.security)
        assertEquals(NetworkType.MASQUE.type, profile?.network)
        assertEquals("h3", profile?.alpn)
        assertEquals("172.16.0.2", profile?.localAddress)
        assertEquals("1.1.1.1", profile?.remoteDNS)
        assertEquals("{\"a\":\"b\"}", profile?.headers)
    }

    @Test
    fun parseRejectsUnknownAlpn() {
        assertNull(MasqueFmt.parse("masque://secret@example.com:443?publickey=pub&alpn=http%2F1.1"))
    }

    @Test
    fun uriRoundTripKeepsMasqueFields() {
        val profile = MasqueFmt.parse(
            "masque://secret@example.com:443?publickey=pub&address=172.16.0.2&dns=1.1.1.1"
        )!!
        val exported = "masque://${MasqueFmt.toUri(profile)}"
        val reparsed = MasqueFmt.parse(exported)!!

        assertEquals("example.com", reparsed.server)
        assertEquals("secret", reparsed.secretKey)
        assertEquals("pub", reparsed.publicKey)
        assertEquals("172.16.0.2", reparsed.localAddress)
        assertEquals("1.1.1.1", reparsed.remoteDNS)
        assertEquals("h3", reparsed.alpn)
    }
}