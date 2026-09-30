package com.v2ray.md.handler

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
