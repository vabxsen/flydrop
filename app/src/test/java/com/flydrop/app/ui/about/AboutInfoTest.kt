package com.flydrop.app.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class AboutInfoTest {

    @Test
    fun linksHangOffTheConfiguredRepository() {
        assertEquals("https://example.com/acme/app/issues", info().issuesUrl)
        assertTrue(info().newIssueUrl.startsWith("https://example.com/acme/app/issues/new?body="))
    }

    /** A maintainer should not have to ask which build a report came from. */
    @Test
    fun theBugReportCarriesTheBuildAndDevice() {
        val body = decodedBody(info())

        assertTrue(body, body.contains("FlyDrop 2.4.0 (build 17)"))
        assertTrue(body, body.contains("release"))
        assertTrue(body, body.contains("Android 16 (API 36)"))
        assertTrue(body, body.contains("Google Pixel 9"))
    }

    @Test
    fun theBugReportSaysWhenItIsADebugBuild() {
        assertTrue(decodedBody(info(debugBuild = true)).contains("debug"))
    }

    /** The template is the point: an empty box gets an empty report. */
    @Test
    fun theBugReportOffersPromptsToFillIn() {
        val body = decodedBody(info())

        assertTrue(body, body.contains("What happened?"))
        assertTrue(body, body.contains("Steps to reproduce"))
    }

    /**
     * The body is markdown with newlines and `*`, none of which survive a query
     * string unencoded.
     */
    @Test
    fun theBodyIsEncodedForAQueryString() {
        val encoded = info().newIssueUrl.substringAfter("body=")

        assertTrue(encoded, !encoded.contains("\n"))
        assertTrue(encoded, !encoded.contains("#"))
        assertTrue(encoded, !encoded.contains(" "))
    }

    private fun decodedBody(info: AboutInfo): String =
        URLDecoder.decode(info.newIssueUrl.substringAfter("body="), "UTF-8")

    private fun info(debugBuild: Boolean = false) = AboutInfo(
        versionName = "2.4.0",
        versionCode = 17,
        packageName = "com.flydrop.app",
        debugBuild = debugBuild,
        sourceUrl = "https://example.com/acme/app",
        androidRelease = "16",
        sdkInt = 36,
        device = "Google Pixel 9",
    )
}
