package com.flydrop.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AppReleaseTest {

    /**
     * Trimmed from a real response for this project's own repository, so a
     * field GitHub renames cannot pass unnoticed.
     */
    private val realPayload = """
        {
          "tag_name": "v1.0.1",
          "name": "FlyDrop v1.0.1",
          "body": "Phone contacts and interaction fixes.",
          "html_url": "https://github.com/vabxsen/flydrop/releases/tag/v1.0.1",
          "assets": [
            {
              "name": "FlyDrop-v1.0.1.apk",
              "size": 2965982,
              "content_type": "application/vnd.android.package-archive",
              "browser_download_url": "https://github.com/vabxsen/flydrop/releases/download/v1.0.1/FlyDrop-v1.0.1.apk"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun readsARealReleasePayload() {
        val release = parseRelease(realPayload)

        assertEquals("v1.0.1", release.tag)
        assertEquals("FlyDrop v1.0.1", release.name)
        assertEquals("Phone contacts and interaction fixes.", release.notes)
        assertEquals("https://github.com/vabxsen/flydrop/releases/tag/v1.0.1", release.pageUrl)
        assertEquals(
            "https://github.com/vabxsen/flydrop/releases/download/v1.0.1/FlyDrop-v1.0.1.apk",
            release.apkUrl,
        )
        assertEquals(2_965_982L, release.apkBytes)
    }

    /** A release with only source archives has nothing this app can install. */
    @Test
    fun aReleaseWithNoApkReportsNoDownload() {
        val release = parseRelease(
            """{"tag_name":"v2.0.0","assets":[{"name":"notes.txt","browser_download_url":"https://x/n.txt"}]}""",
        )

        assertNull(release.apkUrl)
        assertEquals(0L, release.apkBytes)
    }

    @Test
    fun anApkIsFoundPastOtherAssets() {
        val release = parseRelease(
            """
            {"tag_name":"v2.0.0","assets":[
              {"name":"checksums.txt","browser_download_url":"https://x/c.txt","size":10},
              {"name":"FlyDrop-v2.0.0.APK","browser_download_url":"https://x/a.apk","size":42}
            ]}
            """.trimIndent(),
        )

        assertEquals("https://x/a.apk", release.apkUrl)
        assertEquals(42L, release.apkBytes)
    }

    @Test
    fun missingOptionalFieldsFallBackToTheTag() {
        val release = parseRelease("""{"tag_name":"v3.1.0"}""")

        assertEquals("v3.1.0", release.tag)
        assertEquals("v3.1.0", release.name)
        assertEquals("", release.notes)
        assertNull(release.apkUrl)
    }

    /** Without a tag there is no version to compare, so this must not be guessed at. */
    @Test
    fun aPayloadWithNoTagIsRejected() {
        assertThrows(Exception::class.java) { parseRelease("""{"name":"nameless"}""") }
        assertThrows(Exception::class.java) { parseRelease("not json at all") }
    }

    /** The end-to-end decision the update check actually makes. */
    @Test
    fun theInstalledBuildIsNotOfferedItsOwnRelease() {
        val release = parseRelease(realPayload)

        assertEquals(false, AppVersion.isNewer(release.tag, "1.0.1"))
        assertEquals(true, AppVersion.isNewer(release.tag, "1.0.0"))
    }

    @Test
    fun aNewerReleaseWithoutAnApkIsNotOfferedForInstallation() {
        val release = parseRelease("""{"tag_name":"v2.0.0","assets":[]}""")

        val result = classifyUpdate(release, "1.0.2")

        assertTrue(result is UpdateCheck.Failed)
        assertTrue((result as UpdateCheck.Failed).message.contains("no APK"))
    }

    @Test
    fun aNewerReleaseWithAnApkIsOffered() {
        val release = parseRelease(realPayload)

        assertTrue(classifyUpdate(release, "1.0.0") is UpdateCheck.Available)
    }
}
