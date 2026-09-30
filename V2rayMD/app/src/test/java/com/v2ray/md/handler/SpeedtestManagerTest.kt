package com.v2ray.md.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

class SpeedtestManagerTest {

    private val noopLog: (String, Throwable?) -> Unit = { _, _ -> }

    @Test
    fun `normalizePingHost trims whitespace`() {
        assertEquals("example.com", SpeedtestManager.normalizePingHost("  example.com\n"))
    }

    @Test
    fun `normalizePingHost strips one pair of ipv6 brackets`() {
        assertEquals("::1", SpeedtestManager.normalizePingHost("[::1]"))
        assertEquals("2001:db8::1", SpeedtestManager.normalizePingHost("  [2001:db8::1]  "))
    }

    @Test
    fun `normalizePingHost keeps unbracketed input as is`() {
        assertEquals("example.com", SpeedtestManager.normalizePingHost("example.com"))
        assertEquals("", SpeedtestManager.normalizePingHost("   "))
        assertEquals("[broken", SpeedtestManager.normalizePingHost("[broken"))
    }

    @Test
    fun `socketConnectTime rejects invalid input without probing`() {
        var attempts = 0
        val connect: (InetAddress, Int, Int) -> Unit = { _, _, _ -> attempts++ }

        assertEquals(-1, SpeedtestManager.socketConnectTime("", 443, 1000, connect, noopLog))
        assertEquals(-1, SpeedtestManager.socketConnectTime("   ", 443, 1000, connect, noopLog))
        assertEquals(-1, SpeedtestManager.socketConnectTime("example.com", 0, 1000, connect, noopLog))
        assertEquals(-1, SpeedtestManager.socketConnectTime("example.com", 65536, 1000, connect, noopLog))
        assertEquals(-1, SpeedtestManager.socketConnectTime("example.com", 443, 0, connect, noopLog))
        assertEquals(0, attempts)
    }

    @Test(timeout = 15000)
    fun `socketConnectTime returns minus one for unknown host`() {
        var attempts = 0
        val connect: (InetAddress, Int, Int) -> Unit = { _, _, _ -> attempts++ }

        assertEquals(-1, SpeedtestManager.socketConnectTime("nonexistent.invalid", 443, 2000, connect, noopLog))
        assertEquals(0, attempts)
    }

    @Test
    fun `socketConnectTime tries every resolved address before failing`() {
        val expectedAttempts = InetAddress.getAllByName("localhost").size
        var attempts = 0
        val connect: (InetAddress, Int, Int) -> Unit = { _, _, _ ->
            attempts++
            throw IOException("simulated refuse")
        }

        assertEquals(-1, SpeedtestManager.socketConnectTime("localhost", 9, 2000, connect, noopLog))
        assertEquals(expectedAttempts, attempts)
    }

    @Test
    fun `socketConnectTime falls through to the next address after failure`() {
        val expectedAttempts = InetAddress.getAllByName("localhost").size
        var attempts = 0
        val connect: (InetAddress, Int, Int) -> Unit = { _, _, _ ->
            attempts++
            if (attempts < expectedAttempts) {
                throw IOException("simulated refuse")
            }
        }

        val result = SpeedtestManager.socketConnectTime("localhost", 9, 2000, connect, noopLog)
        assertTrue(result >= 0)
        assertEquals(expectedAttempts, attempts)
    }

    @Test
    fun `socketConnectTime measures a reachable loopback port`() {
        ServerSocket(0).use { server ->
            val result = SpeedtestManager.socketConnectTime("127.0.0.1", server.localPort, 3000)
            assertTrue(result >= 0)
        }
    }

    @Test
    fun `extractCustomTcpEndpoint reads vless proxy outbound`() {
        val raw = """
            {
              "outbounds": [
                {"tag": "direct", "protocol": "freedom"},
                {"tag": "proxy", "protocol": "vless",
                 "settings": {"vnext": [{"address": "185.155.223.24", "port": 443}]}},
                {"tag": "block", "protocol": "blackhole"}
              ]
            }
        """.trimIndent()

        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("185.155.223.24", 443),
            SpeedtestManager.extractCustomTcpEndpoint(raw)
        )
    }

    @Test
    fun `extractCustomTcpEndpoint prefers proxy tag over first outbound`() {
        val raw = """
            {
              "outbounds": [
                {"tag": "direct", "protocol": "freedom"},
                {"tag": "proxy", "protocol": "trojan",
                 "settings": {"servers": [{"address": "trojan.example.com", "port": 8443}]}}
              ]
            }
        """.trimIndent()

        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("trojan.example.com", 8443),
            SpeedtestManager.extractCustomTcpEndpoint(raw)
        )
    }

    @Test
    fun `extractCustomTcpEndpoint reads vmess and shadowsocks shapes`() {
        val vmess = """
            {"outbounds": [{"tag": "proxy", "protocol": "vmess",
             "settings": {"vnext": [{"address": "vmess.example.com", "port": 8388}]}}]}
        """.trimIndent()
        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("vmess.example.com", 8388),
            SpeedtestManager.extractCustomTcpEndpoint(vmess)
        )

        val shadowsocks = """
            {"outbounds": [{"tag": "proxy", "protocol": "shadowsocks",
             "settings": {"servers": [{"address": "ss.example.com", "port": 8389}]}}]}
        """.trimIndent()
        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("ss.example.com", 8389),
            SpeedtestManager.extractCustomTcpEndpoint(shadowsocks)
        )
    }

    @Test
    fun `extractCustomTcpEndpoint splits hysteria2 hostport strings`() {
        val raw = """
            {"outbounds": [{"tag": "proxy", "protocol": "hysteria2",
             "settings": {"servers": ["hy2.example.com:8443"]}}]}
        """.trimIndent()
        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("hy2.example.com", 8443),
            SpeedtestManager.extractCustomTcpEndpoint(raw)
        )

        val bracketed = """
            {"outbounds": [{"tag": "proxy", "protocol": "hysteria2",
             "settings": {"servers": ["[2001:db8::1]:8443"]}}]}
        """.trimIndent()
        assertEquals(
            SpeedtestManager.CustomTcpEndpoint("2001:db8::1", 8443),
            SpeedtestManager.extractCustomTcpEndpoint(bracketed)
        )
    }

    @Test
    fun `extractCustomTcpEndpoint returns null for unsupported or broken configs`() {
        assertNull(SpeedtestManager.extractCustomTcpEndpoint("not json"))
        assertNull(SpeedtestManager.extractCustomTcpEndpoint("{}"))
        assertNull(SpeedtestManager.extractCustomTcpEndpoint("""{"outbounds": []}"""))
        assertNull(
            SpeedtestManager.extractCustomTcpEndpoint(
                """{"outbounds": [{"tag": "proxy", "protocol": "wireguard"}]}"""
            )
        )
        assertNull(
            SpeedtestManager.extractCustomTcpEndpoint(
                """{"outbounds": [{"tag": "proxy", "protocol": "vless",
                    "settings": {"vnext": [{"address": "", "port": 99999}]}}]}"""
            )
        )
        assertNull(
            SpeedtestManager.extractCustomTcpEndpoint(
                """{"outbounds": [{"tag": "proxy", "protocol": "dokodemo-door",
                    "settings": {"address": "x", "port": 123}}]}"""
            )
        )
    }
}
