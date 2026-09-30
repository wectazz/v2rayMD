package com.v2ray.md.handler

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.v2ray.md.AppConfig
import com.v2ray.md.dto.IPAPIInfo
import com.v2ray.md.dto.UrlContentRequest
import com.v2ray.md.util.HttpUtil
import com.v2ray.md.util.JsonUtil
import com.v2ray.md.util.LogUtil
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object SpeedtestManager {

    data class RemoteEndpointInfo(
        val country: String?,
        val ipAddress: String?,
    )

    /**
     * Measures the time taken to establish a TCP connection to a given URL and port.
     *
     * Every resolved address is tried in order within the total [timeoutMs] budget, so a
     * host that resolves to an unreachable address first (IPv6-first DNS, dead CDN edge)
     * no longer reports failure while another address is reachable.
     *
     * @param url The URL to connect to.
     * @param port The port to connect to.
     * @return The connection time in milliseconds, or -1 if the connection failed.
     */
    fun socketConnectTime(url: String, port: Int, timeoutMs: Int = 1500): Long {
        return socketConnectTime(
            url = url,
            port = port,
            timeoutMs = timeoutMs,
            connect = ::connectSocket,
            logError = { message, error ->
                if (error == null) {
                    LogUtil.e(AppConfig.TAG, message)
                } else {
                    LogUtil.e(AppConfig.TAG, message, error)
                }
            },
        )
    }

    internal fun socketConnectTime(
        url: String,
        port: Int,
        timeoutMs: Int,
        connect: (address: InetAddress, port: Int, timeoutMs: Int) -> Unit,
        logError: (message: String, error: Throwable?) -> Unit,
    ): Long {
        val host = normalizePingHost(url)
        if (host.isEmpty() || port <= 0 || port > 65535 || timeoutMs <= 0) {
            return -1
        }

        val startNanos = System.nanoTime()
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (e: UnknownHostException) {
            logError("Unknown host: $host", e)
            return -1
        } catch (e: SecurityException) {
            logError("DNS lookup blocked for TCP ping", e)
            return -1
        } catch (e: Exception) {
            logError("DNS lookup failed for TCP ping", e)
            return -1
        }

        var lastError: Exception? = null
        for (address in addresses) {
            val remainingMs = timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            if (remainingMs <= 0) {
                break
            }
            try {
                connect(address, port, remainingMs.toInt())
                return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            } catch (e: IOException) {
                lastError = e
            } catch (e: SecurityException) {
                lastError = e
            } catch (e: IllegalArgumentException) {
                lastError = e
            }
        }
        logError("socketConnectTime failed: $host:$port (${lastError?.message})", lastError)
        return -1
    }

    /**
     * Normalizes a configured server address for raw socket use: trims whitespace and
     * strips one pair of surrounding IPv6 brackets (`[::1]` -> `::1`), which
     * [InetSocketAddress] would otherwise fail to resolve.
     */
    internal fun normalizePingHost(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length > 2 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length - 1).trim()
        }
        return trimmed
    }

    private fun connectSocket(address: InetAddress, port: Int, timeoutMs: Int) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(address, port), timeoutMs)
        }
    }

    /**
     * TCP endpoint extracted from a raw custom (full JSON) server config.
     */
    internal data class CustomTcpEndpoint(val host: String, val port: Int)

    /**
     * Extracts a TCP-pingable endpoint from a custom server config that carries a full
     * Xray JSON instead of typed host/port fields. Prefers the outbound tagged `proxy`,
     * then tries the remaining outbounds in order. Returns null for UDP-only protocols
     * (wireguard), unknown protocols, or unparsable configs.
     */
    internal fun extractCustomTcpEndpoint(rawJson: String): CustomTcpEndpoint? {
        val root = try {
            JsonParser.parseString(rawJson).asJsonObject
        } catch (_: Exception) {
            return null
        }
        val outbounds = try {
            root.getAsJsonArray("outbounds")
        } catch (_: Exception) {
            return null
        } ?: return null

        val proxyTagged = mutableListOf<JsonObject>()
        val rest = mutableListOf<JsonObject>()
        for (element in outbounds) {
            if (!element.isJsonObject) continue
            val outbound = element.asJsonObject
            if (outbound.string("tag") == "proxy") proxyTagged.add(outbound) else rest.add(outbound)
        }
        for (outbound in proxyTagged + rest) {
            parseOutboundEndpoint(outbound)?.let { return it }
        }
        return null
    }

    private fun parseOutboundEndpoint(outbound: JsonObject): CustomTcpEndpoint? {
        val protocol = outbound.string("protocol")?.lowercase() ?: return null
        val settings = try {
            outbound.getAsJsonObject("settings")
        } catch (_: Exception) {
            return null
        } ?: return null
        return when (protocol) {
            "vless", "vmess" -> {
                val first = settings.jsonObjects("vnext").firstOrNull() ?: return null
                endpointOf(first.string("address"), first.int("port"))
            }

            "trojan", "shadowsocks", "socks", "http" -> {
                val first = settings.jsonObjects("servers").firstOrNull() ?: return null
                endpointOf(first.string("address"), first.int("port"))
            }

            "hysteria", "hysteria2" -> {
                // v2 shape: flat settings.address/settings.port; v1 shape: settings.servers array.
                endpointOf(settings.string("address"), settings.int("port"))
                    ?: settings.jsonObjects("servers")
                        .firstNotNullOfOrNull { endpointOf(it.string("address"), it.int("port")) }
                    ?: settings.jsonStrings("servers")
                        .firstNotNullOfOrNull { splitHostPort(it)?.let { (h, p) -> endpointOf(h, p) } }
            }

            else -> null
        }
    }

    private fun JsonObject.jsonObjects(key: String): List<JsonObject> {
        val array = try {
            getAsJsonArray(key)
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()
        return array.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }
    }

    private fun JsonObject.jsonStrings(key: String): List<String> {
        val array = try {
            getAsJsonArray(key)
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()
        return array.mapNotNull {
            try {
                it.takeIf { element -> element.isJsonPrimitive }
                    ?.asJsonPrimitive?.takeIf { primitive -> primitive.isString }?.asString
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun JsonObject.string(key: String): String? = try {
        get(key)?.takeIf { it.isJsonPrimitive }
            ?.asJsonPrimitive?.takeIf { primitive -> primitive.isString }?.asString
    } catch (_: Exception) {
        null
    }

    private fun JsonObject.int(key: String): Int? = try {
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asInt
    } catch (_: Exception) {
        null
    }

    private fun endpointOf(host: String?, port: Int?): CustomTcpEndpoint? {
        if (host.isNullOrBlank() || port == null || port <= 0 || port > 65535) return null
        val normalized = normalizePingHost(host)
        if (normalized.isEmpty()) return null
        return CustomTcpEndpoint(normalized, port)
    }

    private fun splitHostPort(value: String): Pair<String, Int>? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("[")) {
            val end = trimmed.indexOf(']')
            if (end <= 1) return null
            val port = trimmed.substringAfterLast(':').toIntOrNull() ?: return null
            return trimmed.substring(1, end) to port
        }
        if (!trimmed.contains(':')) return null
        val host = trimmed.substringBeforeLast(':')
        val port = trimmed.substringAfterLast(':').toIntOrNull() ?: return null
        if (host.isEmpty() || host.contains(':')) return null
        return host to port
    }

    fun getRemoteIPInfo(): RemoteEndpointInfo? {
        val url = MmkvManager.decodeSettingsString(AppConfig.PREF_IP_API_URL)
            .takeIf { !it.isNullOrBlank() } ?: AppConfig.IP_API_URL

        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()
        val httpPort = SettingsManager.getHttpPort()
        if (httpPort == 0) return null
        val content = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000,
                httpPort = httpPort,
                proxyUsername = proxyUsername,
                proxyPassword = proxyPassword
            )
        ) ?: return null
        val ipInfo = JsonUtil.fromJsonSafe(content, IPAPIInfo::class.java) ?: return null

        val ip = listOf(
            ipInfo.ip,
            ipInfo.clientIp,
            ipInfo.ip_addr,
            ipInfo.query
        ).firstOrNull { !it.isNullOrBlank() }

        val country = listOf(
            ipInfo.country_code,
            ipInfo.country,
            ipInfo.countryCode,
            ipInfo.location?.country_code
        ).firstOrNull { !it.isNullOrBlank() }

        return RemoteEndpointInfo(
            country = country,
            ipAddress = ip,
        )
    }
}
