package com.v2ray.md.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonetColorsTest {

    @Test
    fun resolveSeed_disabled_returnsNull() {
        assertNull(MonetColors.resolveSeed(false, true, 0xFF000000.toInt(), 0xFF111111.toInt(), 0xFF222222.toInt()))
        assertNull(MonetColors.resolveSeed(false, false, null, null, 0xFF222222.toInt()))
    }

    @Test
    fun resolveSeed_sPlus_prefersSystemAccent() {
        assertEquals(
            0xFF000001.toInt(),
            MonetColors.resolveSeed(true, true, 0xFF000001.toInt(), 0xFF000002.toInt(), 0xFF000003.toInt())
        )
    }

    @Test
    fun resolveSeed_sPlus_withoutAccent_fallsBackToMonetThenFallback() {
        assertEquals(
            0xFF000002.toInt(),
            MonetColors.resolveSeed(true, true, null, 0xFF000002.toInt(), 0xFF000003.toInt())
        )
        assertEquals(
            0xFF000003.toInt(),
            MonetColors.resolveSeed(true, true, null, null, 0xFF000003.toInt())
        )
    }

    @Test
    fun resolveSeed_preS_usesMonetSeedThenFallback() {
        assertEquals(
            0xFF000002.toInt(),
            MonetColors.resolveSeed(true, false, null, 0xFF000002.toInt(), 0xFF000003.toInt())
        )
        assertEquals(
            0xFF000003.toInt(),
            MonetColors.resolveSeed(true, false, 0xFF000001.toInt(), null, 0xFF000003.toInt())
        )
    }
}
