package com.v2ray.md.handler

import org.junit.Assert.assertEquals
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
}
