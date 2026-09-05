package com.flydrop.app.data.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import com.flydrop.app.data.FALLBACK_MIME_TYPE
import com.flydrop.app.data.PickedFile

/**
 * Sending files through Quick Share, and getting to the receiving end of it.
 *
 * There is no public API that targets Quick Share directly. It is not a Settings
 * action, an Intent constant, or a documented component - checked against the
 * android-36 SDK, which exposes neither a Nearby Share settings action nor any
 * Quick Share intent. What Android does document is the Sharesheet: Quick Share
 * registers as an `ACTION_SEND` target, so the supported route is to build a
 * correct share Intent and let the user choose it. That is what this file does.
 *
 * Everything the transfer itself needs - discovering devices, picking a
 * receiver, connecting, confirming, moving the bytes - belongs to Quick Share
 * once the Intent is handed over. The receiver needs no copy of this app,
 * because nothing about the Intent is specific to it.
 */

/** How a share attempt ended, so the UI can say something true about it. */
sealed interface ShareOutcome {
    data object Launched : ShareOutcome

    /** Nothing to send: the selection was empty. */
    data object NothingSelected : ShareOutcome

    data class Failed(val message: String) : ShareOutcome
}

/**
 * Builds the share Intent for [files].
 *
 * Single files use `ACTION_SEND` with one URI in `EXTRA_STREAM`; several use
 * `ACTION_SEND_MULTIPLE` with an `ArrayList`. Both carry
 * [Intent.FLAG_GRANT_READ_URI_PERMISSION], which is what lets Quick Share read
 * files this app does not own - the picker granted them to us, and the flag
 * passes that grant along for the life of the receiving activity.
 *
 * The URIs are also placed in [ClipData]. The platform migrates `EXTRA_STREAM`
 * into ClipData itself when an Intent leaves the process, and the grant is
 * applied from there, but building it explicitly means the Intent is already in
 * the shape the grant machinery expects rather than relying on that fix-up.
 *
 * Returns null for an empty selection, which has no valid Intent.
 */
fun shareIntentFor(files: List<PickedFile>): Intent? {
    if (files.isEmpty()) return null

    val uris = files.map { it.uri }
    val type = commonMimeType(files.map { it.mimeType })

    val intent = if (files.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }

    return intent.apply {
        setType(type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData(
            ClipDescription(
                if (files.size == 1) files.first().name else "${files.size} files",
                arrayOf(type),
            ),
            ClipData.Item(uris.first()),
        ).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
    }
}

/**
 * The narrowest MIME type that still covers every file.
 *
 * The Sharesheet filters targets on this, so it has to be wide enough to
 * describe all of them and no wider. Three JPEGs stay "image/jpeg"; a JPEG
 * beside a PNG widens to the image wildcard; a photo beside a PDF has nothing
 * in common and falls back to the fully wild type.
 */
fun commonMimeType(types: List<String>): String {
    val present = types.filter { it.isNotBlank() }
    if (present.isEmpty()) return FALLBACK_MIME_TYPE

    present.distinct().singleOrNull()?.let { return it }

    val categories = present.map { it.substringBefore('/') }.distinct()
    return categories.singleOrNull()?.let { "$it/*" } ?: "*/*"
}

/**
 * Opens the Sharesheet for [files], where Quick Share appears alongside every
 * other target that accepts them.
 *
 * The chooser is used rather than a bare `ACTION_SEND` so the user always gets
 * the picker - including Quick Share - instead of whatever default the phone
 * happens to hold, and so nothing is needed from any particular manufacturer.
 * Samsung, Pixel, OnePlus, Xiaomi and the rest each ship their own Quick Share
 * implementation; all of them register as share targets, so none of them needs
 * a special case here.
 */
fun shareFiles(context: Context, files: List<PickedFile>): ShareOutcome {
    val intent = shareIntentFor(files) ?: return ShareOutcome.NothingSelected

    val chooser = Intent.createChooser(intent, "Send with Quick Share").apply {
        // The chooser passes the grant on to whichever target is picked, so the
        // flag has to survive the wrapping too.
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return try {
        context.startActivity(chooser)
        ShareOutcome.Launched
    } catch (_: ActivityNotFoundException) {
        ShareOutcome.Failed("This phone has no app that can share files.")
    } catch (_: SecurityException) {
        // Thrown when a grant can no longer be passed on - typically the picker
        // permission expired because the process was restarted since the pick.
        ShareOutcome.Failed("Those files are no longer available. Pick them again.")
    }
}

/**
 * Opens a manufacturer-provided Quick Share receiving screen when available.
 *
 * No public Android API switches receiving on or even exposes a universal
 * settings page. Generic wireless/Bluetooth settings are deliberately not used
 * as fallbacks because opening one does not complete the requested action.
 *
 * Each is launched rather than resolved first, because `resolveActivity` is
 * subject to package-visibility filtering on this target SDK and would report a
 * false absence without a `<queries>` entry, while launching is not filtered.
 */
fun openQuickShareReceive(context: Context): Boolean =
    quickShareReceiveIntents().any { context.startActivitySafely(it) }

/**
 * Where to look for Quick Share's own screen, nearest first.
 *
 * These intents are published by Google Play services and Samsung. Neither is
 * part of the Android SDK, so both are tried and allowed to fail; callers then
 * show accurate instructions instead of claiming an unrelated settings page
 * is Quick Share.
 */
internal fun quickShareReceiveIntents(): List<Intent> = listOf(
    Intent("com.google.android.gms.settings.NEARBY_SHARING_SETTINGS"),
    Intent("com.samsung.android.intent.action.QUICK_SHARE_SETTINGS"),
)

private fun Context.startActivitySafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
