package com.flydrop.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun tagsParseWithOrWithoutTheirPrefix() {
        assertEquals(AppVersion(listOf(1, 0, 2), null), AppVersion.parse("v1.0.2"))
        assertEquals(AppVersion(listOf(1, 0, 2), null), AppVersion.parse("1.0.2"))
        assertEquals(AppVersion(listOf(1, 0, 2), null), AppVersion.parse("  V1.0.2 "))
    }

    @Test
    fun preReleaseAndBuildMetadataAreSeparatedFromTheNumbers() {
        assertEquals(AppVersion(listOf(1, 2, 0), "beta.1"), AppVersion.parse("v1.2.0-beta.1"))
        assertEquals(AppVersion(listOf(1, 2, 0), null), AppVersion.parse("1.2.0+build7"))
    }

    @Test
    fun nonsenseDoesNotParse() {
        listOf("", "   ", "v", "latest", "1.0.x", "1..0", "-1.0", "1.0.2-")
            .forEach { assertNull("expected $it not to parse", AppVersion.parse(it)) }
    }

    @Test
    fun missingSegmentsCountAsZero() {
        assertEquals(0, AppVersion.parse("1.2")!!.compareTo(AppVersion.parse("1.2.0")!!))
        assertTrue(AppVersion.parse("1.2.1")!! > AppVersion.parse("1.2")!!)
    }

    @Test
    fun segmentsCompareNumericallyNotAsText() {
        // The bug this guards: "10" sorts before "9" as a string.
        assertTrue(AppVersion.parse("1.10.0")!! > AppVersion.parse("1.9.0")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.99.99")!!)
    }

    /** Semver's rule: a pre-release leads to a release, so it is older. */
    @Test
    fun aPreReleaseIsOlderThanTheReleaseItLeadsTo() {
        assertTrue(AppVersion.parse("1.2.0")!! > AppVersion.parse("1.2.0-beta")!!)
        assertTrue(AppVersion.parse("1.2.0-beta")!! < AppVersion.parse("1.2.0")!!)
        assertTrue(AppVersion.parse("1.2.0-beta.2")!! > AppVersion.parse("1.2.0-beta.1")!!)
    }

    @Test
    fun anUpdateIsOfferedOnlyForSomethingNewer() {
        assertTrue(AppVersion.isNewer("v1.0.2", "1.0.1"))
        assertTrue(AppVersion.isNewer("v2.0.0", "1.9.9"))

        assertFalse(AppVersion.isNewer("v1.0.1", "1.0.1"))
        assertFalse(AppVersion.isNewer("v1.0.0", "1.0.1"))
    }

    /**
     * An unreadable tag must never be treated as newer: offering an install on
     * the strength of a version nobody could parse is worse than staying quiet.
     */
    @Test
    fun anUnreadableVersionIsNeverOffered() {
        assertFalse(AppVersion.isNewer("latest", "1.0.1"))
        assertFalse(AppVersion.isNewer("nightly-build", "1.0.1"))
        assertFalse(AppVersion.isNewer("v1.0.2", "not-a-version"))
        assertFalse(AppVersion.isNewer("", ""))
    }

    @Test
    fun roundTripsThroughItsOwnText() {
        listOf("1.0.2", "1.2.0-beta.1", "3.4").forEach { raw ->
            assertEquals(raw, AppVersion.parse(raw).toString())
        }
    }
}
