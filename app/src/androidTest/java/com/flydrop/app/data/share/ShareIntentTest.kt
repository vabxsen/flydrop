package com.flydrop.app.data.share

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flydrop.app.data.PickedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The share Intent is the whole integration: everything after it belongs to
 * Quick Share. These assert the parts Quick Share and the Sharesheet actually
 * read - action, stream extra, type, and the read grant that lets another app
 * open files this one does not own.
 */
@RunWith(AndroidJUnit4::class)
class ShareIntentTest {

    private fun file(name: String, type: String) = PickedFile(
        uri = Uri.parse("content://com.example.provider/doc/$name"),
        name = name,
        sizeBytes = 1_024L,
        mimeType = type,
    )

    @Test
    fun oneFileUsesActionSendWithASingleStream() {
        val picked = file("holiday.jpg", "image/jpeg")

        val intent = shareIntentFor(listOf(picked))

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SEND, intent!!.action)
        assertEquals("image/jpeg", intent.type)

        @Suppress("DEPRECATION")
        val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(picked.uri, stream)
    }

    @Test
    fun severalFilesUseActionSendMultipleWithEveryUri() {
        val files = listOf(
            file("one.jpg", "image/jpeg"),
            file("two.png", "image/png"),
            file("three.pdf", "application/pdf"),
        )

        val intent = shareIntentFor(files)!!

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        // Nothing is common to an image and a PDF but the wildcard.
        assertEquals("*/*", intent.type)

        @Suppress("DEPRECATION")
        val streams = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals(files.map { it.uri }, streams)
    }

    @Test
    fun everyShareGrantsReadAccessToTheReceiver() {
        val intent = shareIntentFor(listOf(file("a.jpg", "image/jpeg")))!!

        assertTrue(
            "Quick Share cannot open the file without the read grant",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
    }

    /**
     * The grant is applied from ClipData when the Intent leaves the process, so
     * every shared URI has to appear there as well as in EXTRA_STREAM.
     */
    @Test
    fun everyUriAlsoAppearsInClipDataSoTheGrantCoversIt() {
        val files = listOf(file("one.jpg", "image/jpeg"), file("two.jpg", "image/jpeg"))

        val clip = shareIntentFor(files)!!.clipData

        assertNotNull(clip)
        assertEquals(files.size, clip!!.itemCount)
        assertEquals(
            files.map { it.uri },
            (0 until clip.itemCount).map { clip.getItemAt(it).uri },
        )
    }

    @Test
    fun anEmptySelectionHasNoIntent() {
        assertNull(shareIntentFor(emptyList()))
    }

    /** Every candidate must be launchable; the last two ship on every build. */
    @Test
    fun receiveFallsBackToScreensThatAlwaysExist() {
        val actions = quickShareReceiveIntents().map { it.action }

        assertTrue(actions.contains(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
        assertTrue(actions.contains(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
    }
}
