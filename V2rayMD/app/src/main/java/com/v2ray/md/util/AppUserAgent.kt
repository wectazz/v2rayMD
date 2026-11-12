package com.v2ray.md.util

import android.os.Build
import com.v2ray.md.BuildConfig

/**
 * Identity sent to subscription panels.
 *
 * Panels map the `User-Agent` prefix to a known client and serve client-specific extras (for
 * example v2rayn:// policy groups) only to recognised ones; `v2rayMD` is not in those lists, so
 * the default keeps the upstream `v2rayNG` token. Device details travel separately in the
 * `x-device-os`, `x-ver-os` and `x-device-model` headers next to `X-HWID`, the same set Throne
 * and Remnawave panels expect.
 */
object AppUserAgent {

    val value: String by lazy { "v2rayNG/${BuildConfig.VERSION_NAME}" }

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