package com.v2ray.md.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HwidManagerTest {

    @Test
    fun `override wins over the android id`() {
        assertEquals("custom", HwidManager.resolve("custom", "android-id"))
        assertEquals("custom", HwidManager.resolve("  custom  ", "android-id"))
    }

    @Test
    fun `blank override falls back to the android id`() {
        assertEquals("android-id", HwidManager.resolve(null, "android-id"))
        assertEquals("android-id", HwidManager.resolve("", "android-id"))
        assertEquals("android-id", HwidManager.resolve("   ", "android-id"))
        assertNull(HwidManager.resolve("", null))
        assertNull(HwidManager.resolve(null, "  "))
    }

    @Test
    fun `generated value is a random uuid`() {
        val first = HwidManager.generate()
        val second = HwidManager.generate()

        assertTrue(
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(first)
        )
        assertNotEquals(first, second)
    }

    @Test
    fun `validation rejects long and control-character values`() {
        assertTrue(HwidManager.isValid(""))
        assertTrue(HwidManager.isValid("a".repeat(HwidManager.MAX_LENGTH)))
        assertFalse(HwidManager.isValid("a".repeat(HwidManager.MAX_LENGTH + 1)))
        assertFalse(HwidManager.isValid("bad\nvalue"))
        assertFalse(HwidManager.isValid("bad\u0000value"))
    }
}