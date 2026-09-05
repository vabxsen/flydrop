package com.flydrop.app.data.update

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/** A published release, reduced to the parts an update needs. */
@Immutable
data class AppRelease(
    /** The tag as published, e.g. `v1.0.2`. */
    val tag: String,
    val name: String,
    val notes: String,
    val pageUrl: String,
    /** Null when the release carries no APK to install. */
    val apkUrl: String?,
    val apkBytes: Long,
)

/**
 * Reads GitHub's release JSON.
 *
 * Kept out of [UpdateRepository] and free of Android so it can be tested
 * against a real API payload: this is where a silent failure would hide, since
 * a field name that stopped matching would quietly look like a release with no
 * APK rather than like an error.
 *
 * Throws when the payload has no `tag_name`, which is the one field an update
 * cannot proceed without.
 */
internal fun parseRelease(body: String): AppRelease {
    val json = JSONObject(body)
    val tag = json.getString("tag_name")

    val assets = json.optJSONArray("assets")
    var apkUrl: String? = null
    var apkBytes = 0L
    for (index in 0 until (assets?.length() ?: 0)) {
        val asset = assets?.optJSONObject(index) ?: continue
        // By extension rather than content_type: the type is set by whoever
        // uploaded the asset and is not always the APK media type.
        if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
        apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
        apkBytes = asset.optLong("size")
        break
    }

    return AppRelease(
        tag = tag,
        name = json.optString("name").ifBlank { tag },
        notes = json.optString("body").trim(),
        pageUrl = json.optString("html_url"),
        apkUrl = apkUrl,
        apkBytes = apkBytes,
    )
}
