package com.flydrop.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable

/** A file the user chose to send, as returned by the system document picker. */
@Immutable
data class PickedFile(
    val uri: Uri,
    val name: String,
    /** Null when the provider does not report a size, which is allowed. */
    val sizeBytes: Long?,
)

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
                PickedFile(uri = uri, name = name.orEmpty(), sizeBytes = size)
            }
        }.getOrNull()

        described?.takeIf { it.name.isNotEmpty() }
            ?: PickedFile(
                uri = uri,
                name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
                    .ifEmpty { "Unnamed file" },
                sizeBytes = described?.sizeBytes,
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
