package com.flydrop.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable

/** A file the user chose to send, as returned by the system document picker. */
@Immutable
data class PickedFile(
    val uri: Uri,
    val name: String,
    /** Null when the provider does not report a size, which is allowed. */
    val sizeBytes: Long?,
    /**
     * What the provider says this is, falling back to the extension and then to
     * [FALLBACK_MIME_TYPE]. Resolved when the file is picked so sharing does not
     * have to query the provider again.
     */
    val mimeType: String = FALLBACK_MIME_TYPE,
)

/** What an unidentifiable file is called. Every Sharesheet target accepts it. */
const val FALLBACK_MIME_TYPE = "application/octet-stream"

/**
 * The provider's own answer where there is one, otherwise a guess from the file
 * extension.
 *
 * A provider is entitled to return null, and some return the useless
 * `application/octet-stream` for everything, so the extension is consulted
 * whenever the answer carries no information. Getting this right matters: the
 * Sharesheet filters its targets on the MIME type, so a photo typed as
 * octet-stream loses the targets that only accept images.
 */
fun ContentResolver.resolveMimeType(uri: Uri, fileName: String): String {
    val reported = runCatching { getType(uri) }.getOrNull()
    if (!reported.isNullOrBlank() && reported != FALLBACK_MIME_TYPE) return reported

    val extension = fileName.substringAfterLast('.', "").lowercase()
    val fromExtension = extension
        .takeIf { it.isNotEmpty() }
        ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }

    return fromExtension ?: reported?.takeIf { it.isNotBlank() } ?: FALLBACK_MIME_TYPE
}

/**
 * Resolves display names and sizes for picked files.
 *
 * A `content://` URI carries neither, so each one has to be queried. A provider
 * is free to report neither, and some report only a name, so both columns are
 * treated as optional and the URI's last path segment is the fallback label -
 * better than showing the user a bare content URI.
 *
 * Runs off the main thread: this is a ContentResolver query per file.
 */
fun ContentResolver.describePickedFiles(uris: List<Uri>): List<PickedFile> =
    uris.map { uri ->
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val described = runCatching {
            query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null

                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    cursor.getString(nameIndex)?.trim()
                } else {
                    null
                }
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else {
                    null
                }
                name.orEmpty() to size
            }
        }.getOrNull()

        val name = described?.first?.takeIf { it.isNotEmpty() }
            ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifEmpty { "Unnamed file" }

        PickedFile(
            uri = uri,
            name = name,
            sizeBytes = described?.second,
            mimeType = resolveMimeType(uri, name),
        )
    }

/** Compact size label, e.g. "4.2 MB". Null sizes read as an em dash. */
fun formatFileSize(bytes: Long?): String {
    if (bytes == null) return "—"
    if (bytes < 1024) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (value >= 10) {
        "${value.toInt()} ${units[unitIndex]}"
    } else {
        String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
    }
}
