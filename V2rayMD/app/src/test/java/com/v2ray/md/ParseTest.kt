package com.v2ray.md

import org.junit.Test
import com.v2ray.md.fmt.CustomFmt

class ParseTest {
    @Test
    fun testParse() {
        val content = "{ \"dns\": {}, \"outbounds\": [{\"tag\": \"Main_Balancer\", \"protocol\": \"vmess\"}], \"routing\": {}, \"remarks\": \"🇪🇺 🌐 АВТО | Европа (базовые)\" }"
        val config = CustomFmt.parse(content)
        println("Config remarks: ${config.remarks}")
        println("Config server: ${config.server}")
        println("Config port: ${config.serverPort}")
    }
}
