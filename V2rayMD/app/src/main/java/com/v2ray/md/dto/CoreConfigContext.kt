package com.v2ray.md.dto

import android.content.Context
import com.v2ray.md.dto.entities.ProfileEntry
import com.v2ray.md.enums.CoreResolvedType

data class CoreConfigContext(
    val context: Context,
    val guid: String,
    val isCustom: Boolean = false,
    val resolvedOutbounds: List<ResolvedOutbound> = emptyList(),
    val routingDomainRules: List<RoutingDomainRule> = emptyList(),
) {
    data class ResolvedOutbound(
        val tag: String,
        val entry: ProfileEntry,
        val resolvedEntries: List<ProfileEntry>,
        val resolvedType: CoreResolvedType,
    )

    data class RoutingDomainRule(
        val domain: List<String>,
        val outboundTag: String,
    )
}
