package com.v2ray.md.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckerManagerTest {

    private fun cmp(v1: String, v2: String): Int =
        UpdateCheckerManager.compareVersions(v1, v2)

    @Test
    fun `plain versions compare numerically`() {
        assertTrue(cmp("2.3.10", "2.3.9") > 0)
        assertTrue(cmp("2.3.9", "2.3.10") < 0)
        assertEquals(0, cmp("2.3.9", "2.3.9"))
        assertEquals(0, cmp("2.3", "2.3.0"))
    }

    @Test
    fun `pre-release build is newer than previous release`() {
        assertTrue(cmp("2.3.11_1", "2.3.10") > 0)
        assertTrue(cmp("2.3.10", "2.3.11_1") < 0)
    }

    @Test
    fun `pre-release builds order lexicographically`() {
        assertTrue(cmp("2.3.11_2", "2.3.11_1") > 0)
        assertTrue(cmp("2.3.11_1", "2.3.11_2") < 0)
        assertEquals(0, cmp("2.3.11_1", "2.3.11_1"))
    }

    @Test
    fun `final release is newer than its pre-releases`() {
        assertTrue(cmp("2.3.11", "2.3.11_2") > 0)
        assertTrue(cmp("2.3.11_2", "2.3.11") < 0)
    }

    @Test
    fun `non-numeric tags never crash`() {
        assertEquals(0, cmp("2.3.9_1", "2.3.9_1"))
        // Non-numeric components count as zero instead of throwing.
        assertEquals(0, cmp("2.3.beta", "2.3.beta"))
        assertTrue(cmp("2.3.1", "2.3.beta") > 0)
    }
}
