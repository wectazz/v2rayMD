package com.v2ray.md.util

import android.os.Build
import com.v2ray.md.BuildConfig

/**
 * Identity sent to subscription panels.
 *
 * The UA carries the client name, app version and platform — the format the panel matches against
 * to decide which subscription flavour to serve: `v2rayMD/<version>/Android`. Device details
 * travel separately in the `x-device-os`, `x-ver-os` and `x-device-model` headers next to
 * `X-HWID`, the same set Throne and Remnawave panels expect.
 */
object AppUserAgent {

    val value: String by lazy { userAgent(BuildConfig.VERSION_NAME) }

    /** Matches the panel's expected `client/version/platform` shape. */
    fun userAgent(version: String): String = "v2rayMD/$version/Android"

    val os: String = "Android"

    val osVersion: String
        get() = sanitize(Build.VERSION.RELEASE).ifEmpty { "unknown" }

    val model: String
        get() = deviceLabel(Build.MANUFACTURER, Build.MODEL)

    /**
     * `manufacturer model` for the `x-device-model` header. Header values must stay single-line
     * and must not break the `;`/`()` structure of surrounding headers, so parts are sanitized.
     */
    fun deviceLabel(manufacturer: String?, model: String?): String =
        listOf(sanitize(manufacturer), sanitize(model))
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(" ")

    private fun sanitize(raw: String?): String =
        raw.orEmpty()
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(";", ",")
            .replace(Regex("\\s+"), " ")
            .trim()
}