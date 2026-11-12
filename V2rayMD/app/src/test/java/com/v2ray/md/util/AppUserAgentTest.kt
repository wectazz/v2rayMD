package com.v2ray.md.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppUserAgentTest {

    @Test
    fun formatCarriesClientVersionAndDevice() {
        assertEquals(
            "v2rayMD/2.3.15 (Linux; Android 15; Google Pixel 7)",
            AppUserAgent.format("2.3.15", "Google", "Pixel 7", "15"),
        )
    }

    @Test
    fun repeatedManufacturerIsNotDuplicated() {
        assertEquals(
            "v2rayMD/2.3.15 (Linux; Android 15; Google)",
            AppUserAgent.format("2.3.15", "Google", "google", "15"),
        )
    }

    @Test
    fun missingFieldsAreDropped() {
        assertEquals(
            "v2rayMD/2.3.15 (Linux; Android unknown)",
            AppUserAgent.format("2.3.15", null, "", null),
        )
    }

    @Test
    fun headerBreakersAreSanitized() {
        assertEquals(
            "v2rayMD/2.3.15 (Linux; Android 15; Acme Phone, X)",
            AppUserAgent.format("2.3.15", "Acme\n", "Phone; X", "15"),
        )
    }
}