package com.v2ray.md.handler

import java.util.UUID

/**
 * Device identifier sent to subscription panels in the `X-HWID` header.
 *
 * A user-provided value overrides the real Android ID, so the panel can be given a
 * different device identity; a blank override means "use the Android ID". Pure
 * functions only, so the behaviour is covered by plain JVM tests.
 */
object HwidManager {
    /** Panels treat the value as an opaque string; keep it bounded and printable. */
    const val MAX_LENGTH = 128

    /** Returns [override] when it is a non-blank value, otherwise the real Android ID. */
    fun resolve(override: String?, androidId: String?): String? =
        override?.trim()?.takeIf { it.isNotEmpty() }
            ?: androidId?.trim()?.takeIf { it.isNotEmpty() }

    /** Random UUID used by the "Generate" button. */
    fun generate(): String = UUID.randomUUID().toString()

    /** True when [value] fits [MAX_LENGTH] and has no control characters. */
    fun isValid(value: String): Boolean =
        value.length <= MAX_LENGTH && value.none { it.isISOControl() }
}