package com.v2ray.md.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomOutboundFmtTest {

    @Test
    fun `outbound tag becomes remarks and raw tag is rewritten to proxy`() {
        val (profile, raw) = CustomOutboundFmt.parse(
            """{"tag":"my-out","protocol":"vless","settings":{"vnext":[{"address":"example.com","port":443}]}}"""
        )

        assertEquals("my-out", profile.remarks)
        assertEquals("example.com", profile.server)
        assertEquals("443", profile.serverPort)
        assertTrue(raw.contains("\"tag\": \"proxy\""), raw)
    }

    @Test
    fun `missing tag falls back to a timestamp remark`() {
        val (profile, _) = CustomOutboundFmt.parse(
            """{"protocol":"socks","settings":{"servers":[{"address":"1.2.3.4","port":1080}]}}"""
        )

        assertTrue(profile.remarks.isNotBlank())
        assertEquals("1.2.3.4", profile.server)
        assertEquals("1080", profile.serverPort)
    }
}