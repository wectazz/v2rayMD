package com.v2ray.md.core

import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.BalancerStrategyType
import com.v2ray.md.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreConfigManagerTest {

    private fun profile(testOutbounds: Boolean? = null, fallbackTag: String? = null) =
        ProfileItem(configType = EConfigType.VMESS).apply {
            policyGroupTestOutbounds = testOutbounds
            policyGroupFallbackTag = fallbackTag
        }

    @Test
    fun `leastPing always falls back to first member`() {
        assertEquals(
            "member-1",
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.LEAST_PING, profile(), "member-1"
            )
        )
    }

    @Test
    fun `leastLoad always falls back to first member`() {
        assertEquals(
            "member-1",
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.LEAST_LOAD, profile(), "member-1"
            )
        )
    }

    @Test
    fun `random uses custom fallback when set`() {
        assertEquals(
            "custom-tag",
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.RANDOM, profile(fallbackTag = "custom-tag"), "member-1"
            )
        )
    }

    @Test
    fun `random defaults to first member without custom fallback`() {
        assertEquals(
            "member-1",
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.RANDOM, profile(), "member-1"
            )
        )
    }

    @Test
    fun `random ignores proxy fallback tag`() {
        assertEquals(
            "member-1",
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.RANDOM, profile(fallbackTag = "proxy"), "member-1"
            )
        )
    }

    @Test
    fun `random has no fallback when testing is disabled`() {
        assertNull(
            CoreConfigManager.resolvePolicyGroupFallbackTag(
                BalancerStrategyType.RANDOM, profile(testOutbounds = false), "member-1"
            )
        )
    }

    @Test
    fun `standard observatory selection`() {
        assertTrue(
            CoreConfigManager.shouldUseStandardObservatory(BalancerStrategyType.LEAST_PING, null)
        )
        assertTrue(
            CoreConfigManager.shouldUseStandardObservatory(BalancerStrategyType.RANDOM, "member-1")
        )
        assertFalse(
            CoreConfigManager.shouldUseStandardObservatory(BalancerStrategyType.RANDOM, null)
        )
        // leastLoad uses the burst observatory instead.
        assertFalse(
            CoreConfigManager.shouldUseStandardObservatory(BalancerStrategyType.LEAST_LOAD, "member-1")
        )
    }
}
