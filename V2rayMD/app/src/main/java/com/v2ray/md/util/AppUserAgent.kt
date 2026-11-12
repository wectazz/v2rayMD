package com.v2ray.md.util

import android.os.Build
import com.v2ray.md.BuildConfig

/**
 * Default identity sent to subscription panels.
 *
 * [value] is the `User-Agent` used when a subscription has no custom one, and [os], [osVersion]
 * and [model] are sent as `x-device-os`, `x-ver-os` and `x-device-model` next to `X-HWID`, the
 * same set Throne and Remnawave panels expect.
 */
object AppUserAgent {

    val value: String by lazy {
        format(
            version = BuildConfig.VERSION_NAME,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidRelease = Build.VERSION.RELEASE,
        )
    }

    val os: String = "Android"

    val osVersion: String
        get() = sanitize(Build.VERSION.RELEASE).ifEmpty { "unknown" }

    val model: String
        get() = sanitize(listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" "))

    /**
     * Builds the default UA. Header values must stay single-line and must not break the
     * `;`/`()` structure, so every part is sanitized first.
     */
    fun format(
        version: String,
        manufacturer: String?,
        model: String?,
        androidRelease: String?,
    ): String {
        val device = listOf(sanitize(manufacturer), sanitize(model))
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .joinToString(" ")
        return buildString {
            append("v2rayMD/").append(sanitize(version))
            append(" (Linux; Android ").append(sanitize(androidRelease).ifEmpty { "unknown" })
            if (device.isNotEmpty()) append("; ").append(device)
            append(")")
        }
    }

    private fun sanitize(raw: String?): String =
        raw.orEmpty()
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(";", ",")
            .replace(Regex("\\s+"), " ")
            .trim()
}