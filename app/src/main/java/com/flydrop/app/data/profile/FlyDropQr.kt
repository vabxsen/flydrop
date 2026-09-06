package com.flydrop.app.data.profile

/** Normalises only FlyDrop QR payloads; arbitrary QR content is never searched. */
fun flyIdFromQr(rawValue: String?): String? {
    val raw = rawValue?.trim().orEmpty()
    val candidate = when {
        raw.startsWith("flydrop:", ignoreCase = true) -> raw.substringAfter(':')
        raw.startsWith(FlyIdRules.PREFIX, ignoreCase = true) -> raw
        else -> return null
    }
    val handle = FlyIdRules.normalise(candidate)
    return handle.takeIf { FlyIdRules.validate(it) == null }
}
