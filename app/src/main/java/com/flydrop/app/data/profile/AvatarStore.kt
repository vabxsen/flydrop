package com.flydrop.app.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
sealed interface AvatarResult {
    data class Success(val image: ImageBitmap) : AvatarResult

    data class Failure(val message: String) : AvatarResult
}

/**
 * Keeps the custom profile photo, one per account, in the app's private
 * no-backup directory.
 *
 * The picked image is copied rather than referenced: a `content://` URI granted
 * by the photo picker is temporary, so holding onto one would leave the avatar
 * working until the next reboot and then quietly vanishing. Copying also lets
 * the image be normalised once — oriented, cropped square and downscaled — so
 * every screen that draws it gets a small, already-correct bitmap.
 *
 * Photos live on the device only. They are not uploaded, so they do not follow
 * the account to another device and peers do not see them.
 */
class AvatarStore(private val context: Context) {

    /** The photo for [key], or null when that account has not set one. */
    suspend fun load(key: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        // openRead also recovers the previous file if the process stopped in
        // the middle of an AtomicFile replacement.
        runCatching {
            AtomicFile(file).openRead().use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    /**
     * Normalises the image at [source] and stores it as the photo for [key],
     * replacing any previous one.
     */
    suspend fun save(key: String, source: Uri): AvatarResult = withContext(Dispatchers.IO) {
        val decoded = runCatching { decodeScaled(source) }.getOrNull()
            ?: return@withContext AvatarResult.Failure("That image could not be read.")

        val square = runCatching { decoded.orientedSquare(source) }
            .getOrElse { return@withContext AvatarResult.Failure("That image could not be prepared.") }

        val written = runCatching { write(key, square) }.getOrDefault(false)
        if (!written) return@withContext AvatarResult.Failure("The photo could not be saved.")

        AvatarResult.Success(square.asImageBitmap())
    }

    suspend fun clear(key: String) = withContext(Dispatchers.IO) {
        fileFor(key).delete()
        Unit
    }

    /**
     * Decodes at roughly [TARGET_PX], never at full size: camera photos run to
     * tens of megapixels and decoding one whole would risk an OutOfMemoryError
     * for an image about to be drawn at 88dp.
     */
    private fun decodeScaled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(source).use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    /**
     * Applies the EXIF rotation, centre-crops to a square and scales to
     * [TARGET_PX]. Cropping here rather than at draw time means every consumer
     * can treat the photo as a square and simply fill its circle with it.
     */
    private fun Bitmap.orientedSquare(source: Uri): Bitmap {
        val rotation = rotationOf(source)
        val upright = if (rotation == 0f) {
            this
        } else {
            Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(rotation) }, true)
        }

        val side = minOf(upright.width, upright.height)
        val cropped = Bitmap.createBitmap(
            upright,
            (upright.width - side) / 2,
            (upright.height - side) / 2,
            side,
            side,
        )
        val target = minOf(side, TARGET_PX)
        return if (target == side) {
            cropped
        } else {
            cropped.scale(target, target)
        }
    }

    private fun rotationOf(source: Uri): Float {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source).use { stream ->
                stream?.let { ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                ) }
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    /**
     * Written to a temporary file and renamed, so an interrupted save leaves
     * the previous photo intact rather than a truncated one.
     */
    private fun write(key: String, bitmap: Bitmap): Boolean {
        val target = fileFor(key)
        return atomicWrite(target) { output ->
            // PNG rather than JPEG: a picked image may carry transparency, and
            // JPEG would flatten it to black.
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    /**
     * One file per account, so signing into a different account on the same
     * device does not show the previous user's photo.
     */
    private fun fileFor(key: String): File {
        val safe = key.filter { it.isLetterOrDigit() }.take(64).ifEmpty { "default" }
        val target = File(File(context.noBackupFilesDir, DIRECTORY), "$safe.png")
        migrateLegacyFile(File(File(context.filesDir, DIRECTORY), "$safe.png"), target)
        return target
    }

    /** Moves photos created by older releases out of backup-eligible storage. */
    private fun migrateLegacyFile(legacy: File, target: File) {
        if (!legacy.exists()) return
        if (target.exists()) {
            legacy.delete()
            return
        }

        val migrated = atomicWrite(target) { output ->
            legacy.inputStream().use { input -> input.copyTo(output) }
            true
        }
        if (migrated) legacy.delete()
    }

    /** [AtomicFile] restores the previous file if writing or syncing fails. */
    private fun atomicWrite(target: File, write: (java.io.FileOutputStream) -> Boolean): Boolean {
        target.parentFile?.mkdirs()
        val atomicFile = AtomicFile(target)
        val output = try {
            atomicFile.startWrite()
        } catch (_: Exception) {
            return false
        }

        return try {
            if (!write(output)) {
                atomicFile.failWrite(output)
                false
            } else {
                atomicFile.finishWrite(output)
                true
            }
        } catch (_: Exception) {
            runCatching { atomicFile.failWrite(output) }
            false
        }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (minOf(width, height) / (sample * 2) >= TARGET_PX) sample *= 2
        return sample
    }

    companion object {
        /** The key for photos set before signing in. */
        const val GUEST_KEY = "guest"

        /**
         * Comfortably above the largest the avatar is ever drawn (88dp, so
         * ~264px on a 3x screen), with room for denser screens.
         */
        private const val TARGET_PX = 512
        private const val DIRECTORY = "avatars"
    }
}
