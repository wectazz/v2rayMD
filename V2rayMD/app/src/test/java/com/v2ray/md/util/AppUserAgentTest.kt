package com.v2ray.md.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppUserAgentTest {

    @Test
    fun deviceLabelJoinsManufacturerAndModel() {
        assertEquals("Xiaomi 2311DRK48G", AppUserAgent.deviceLabel("Xiaomi", "2311DRK48G"))
    }

    @Test
    fun repeatedManufacturerIsNotDuplicated() {
        assertEquals("Google", AppUserAgent.deviceLabel("Google", "google"))
    }

    @Test
    fun missingFieldsAreDropped() {
        assertEquals("", AppUserAgent.deviceLabel(null, ""))
        assertEquals("Pixel 7", AppUserAgent.deviceLabel("", "Pixel 7"))
    }

    @Test
    fun headerBreakersAreSanitized() {
        assertEquals("Acme Phone, X", AppUserAgent.deviceLabel("Acme\n", "Phone; X"))
    }
}