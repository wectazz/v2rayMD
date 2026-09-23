package com.v2ray.md

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import com.v2ray.md.fmt.CustomFmt
import com.v2ray.md.fmt.WireguardFmt

class ParseTest {
    @Test
    fun testParse() {
        val content = "{ \"dns\": {}, \"outbounds\": [{\"tag\": \"Main_Balancer\", \"protocol\": \"vmess\"}], \"routing\": {}, \"remarks\": \"🇪🇺 🌐 АВТО | Европа (базовые)\" }"
        val config = CustomFmt.parse(content)
        println("Config remarks: ${config.remarks}")
        println("Config server: ${config.server}")
        println("Config port: ${config.serverPort}")
    }

    @Test
    fun testWireguardFinalMaskUri() {
        val config = WireguardFmt.parse(
            "wireguard://secret@example.com:51820?address=172.16.0.2%2F32&publickey=pub&fm=mymask#test"
        )
        assertNotNull(config)
        assertEquals("mymask", config?.finalMask)
    }

    @Test
    fun testWireguardFinalMaskConf() {
        val conf = "[Interface]\nPrivateKey=secret\nAddress=172.16.0.2/32\n" +
            "[Peer]\nPublicKey=pub\nEndpoint=example.com:51820\nFinalMask=mymask\n"
        assertEquals("mymask", WireguardFmt.parseWireguardConfFile(conf).finalMask)
    }

    @Test
    fun testWireguardRemoteDnsUri() {
        val config = WireguardFmt.parse(
            "wireguard://secret@example.com:51820?address=172.16.0.2%2F32&publickey=pub&dns=9.9.9.9#test"
        )
        assertNotNull(config)
        assertEquals("9.9.9.9", config?.remoteDNS)
    }

    @Test
    fun testWireguardRemoteDnsConf() {
        val conf = "[Interface]\nPrivateKey=secret\nAddress=172.16.0.2/32\nDNS=9.9.9.9\n" +
            "[Peer]\nPublicKey=pub\nEndpoint=example.com:51820\n"
        assertEquals("9.9.9.9", WireguardFmt.parseWireguardConfFile(conf).remoteDNS)
    }
}
