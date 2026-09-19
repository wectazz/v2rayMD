package com.v2ray.md.fmt

import com.v2ray.md.dto.V2rayConfig
import com.v2ray.md.dto.entities.ProfileItem
import com.v2ray.md.enums.EConfigType
import com.v2ray.md.util.JsonUtil

object CustomFmt : FmtBase() {
    /**
     * Parses a JSON string into a ProfileItem object.
     *
     * @param str the JSON string to parse
     * @return the parsed ProfileItem object, or null if parsing fails
     */
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.CUSTOM)

        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
        val outbound = fullConfig?.getProxyOutbound()

        config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()
        config.network = outbound?.streamSettings?.network
        config.security = outbound?.streamSettings?.security
        config.customProtocol = outbound?.protocol

        return config
    }
}