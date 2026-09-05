package com.flydrop.app.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val keys = mutableSetOf<String>()
    private val sources = mutableListOf<File>()

    @After
    fun cleanUp() {
        keys.forEach { key ->
            val target = File(context.noBackupFilesDir, "avatars/$key.png")
            target.delete()
            File("${target.path}.bak").delete()
            File("${target.path}.new").delete()
            File(context.filesDir, "avatars/$key.png").delete()
        }
        sources.forEach(File::delete)
    }

    @Test
    fun savedAvatarLivesInNoBackupStorage() = runBlocking {
        val key = uniqueKey()
        val source = createPng()

        val result = AvatarStore(context).save(key, Uri.fromFile(source))

        assertTrue(result is AvatarResult.Success)
        assertTrue(File(context.noBackupFilesDir, "avatars/$key.png").exists())
        assertFalse(File(context.filesDir, "avatars/$key.png").exists())
    }

    @Test
    fun legacyAvatarIsMigratedOutOfBackupStorage() = runBlocking {
        val key = uniqueKey()
        val legacy = File(context.filesDir, "avatars/$key.png")
        legacy.parentFile?.mkdirs()
        legacy.outputStream().use { output ->
            createBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        val loaded = AvatarStore(context).load(key)

        assertNotNull(loaded)
        assertTrue(File(context.noBackupFilesDir, "avatars/$key.png").exists())
        assertFalse(legacy.exists())
    }

    @Test
    fun interruptedReplacementRestoresThePreviousAvatar() = runBlocking {
        val key = uniqueKey()
        val store = AvatarStore(context)
        assertTrue(store.save(key, Uri.fromFile(createPng())) is AvatarResult.Success)
        val target = File(context.noBackupFilesDir, "avatars/$key.png")

        // Start a replacement but deliberately never commit it, as if the app
        // process stopped while the new photo was being written.
        AtomicFile(target).startWrite().use { output ->
            output.write(byteArrayOf(1, 2, 3, 4))
        }

        assertNotNull(store.load(key))
        assertTrue(target.exists())
    }

    /**
     * On Android 10 and earlier an interrupted [AtomicFile] write leaves the
     * previous photo in a `.bak` sibling, and openRead recovers from one on
     * every Android version. The backup is written directly here because the
     * emulator this runs on uses the newer `.new` scheme and would not produce
     * one, but a device upgraded from Android 10 still can.
     */
    @Test
    fun removedAvatarStaysRemovedWhenALegacyBackupSurvives() = runBlocking {
        val key = uniqueKey()
        val store = AvatarStore(context)
        assertTrue(store.save(key, Uri.fromFile(createPng())) is AvatarResult.Success)
        val target = File(context.noBackupFilesDir, "avatars/$key.png")
        val backup = File("${target.path}.bak")
        target.copyTo(backup, overwrite = true)

        store.clear(key)

        assertNull(store.load(key))
        assertFalse(target.exists())
        assertFalse(backup.exists())
    }

    private fun uniqueKey(): String = "test${System.nanoTime()}".also(keys::add)

    private fun createPng(): File =
        File(context.cacheDir, "avatar-source-${System.nanoTime()}.png").also { file ->
            sources += file
            file.outputStream().use { output ->
                createBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }

    private fun createBitmap(): Bitmap =
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xFF5C33FD.toInt())
        }
}
