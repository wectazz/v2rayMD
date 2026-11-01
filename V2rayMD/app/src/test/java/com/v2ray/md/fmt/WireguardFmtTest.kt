package com.v2ray.md.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireguardFmtTest {

    @Test
    fun `link without dns defaults to an empty value`() {
        val profile = WireguardFmt.parse("wireguard://secret@example.com:51820?publickey=pub")

        // Empty remoteDNS lets CoreOutboundBuilder pick the built-in defaults; storing
        // the whole comma-separated string there crashed the core on start.
        assertEquals("", profile?.remoteDNS)
    }

    @Test
    fun `link dns is preserved`() {
        val profile = WireguardFmt.parse(
            "wireguard://secret@example.com:51820?publickey=pub&dns=1.1.1.1,9.9.9.9"
        )

        assertEquals("1.1.1.1,9.9.9.9", profile?.remoteDNS)
    }
}