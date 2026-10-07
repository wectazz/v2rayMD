package com.v2ray.md.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonUtilTest {

    @Test
    fun `toJsonPretty keeps fractional numbers`() {
        val json = JsonUtil.toJsonPretty(mapOf("ratio" to 0.4))

        assertTrue(json!!.contains("0.4"), "fraction was truncated: $json")
    }

    @Test
    fun `toJsonPretty writes whole numbers without decimal part`() {
        val json = JsonUtil.toJsonPretty(mapOf("count" to 2.0))

        assertTrue(json!!.contains("\"count\": 2"), "unexpected encoding: $json")
    }

    @Test
    fun `toJsonPretty does not clamp integers above Int range`() {
        val big = 1099511627776.0 // 2^40, outside Int range
        val json = JsonUtil.toJsonPretty(mapOf("big" to big))

        assertTrue(json!!.contains("1099511627776"), "large integer was clamped: $json")
    }

    @Test
    fun `toJsonPretty round-trips mixed arrays`() {
        val json = JsonUtil.toJsonPretty(mapOf("values" to listOf(1.0, 0.5, 1099511627776.0)))

        assertEquals("""{"values":[1,0.5,1099511627776]}""", json?.replace("\\s".toRegex(), ""))
    }
}
