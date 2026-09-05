package com.flydrop.app.data.share

import com.flydrop.app.data.FALLBACK_MIME_TYPE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Sharesheet filters its targets on the type this produces, so a type that
 * is too narrow hides Quick Share and one that is too wide offers targets that
 * cannot open the files.
 */
class CommonMimeTypeTest {

    @Test
    fun oneTypeIsKeptExactly() {
        assertEquals("image/jpeg", commonMimeType(listOf("image/jpeg")))
    }

    @Test
    fun identicalTypesStayNarrow() {
        assertEquals(
            "image/jpeg",
            commonMimeType(listOf("image/jpeg", "image/jpeg", "image/jpeg")),
        )
    }

    @Test
    fun oneCategoryWidensToThatCategory() {
        assertEquals("image/*", commonMimeType(listOf("image/jpeg", "image/png")))
    }

    @Test
    fun mixedCategoriesWidenAllTheWay() {
        assertEquals("*/*", commonMimeType(listOf("image/jpeg", "application/pdf")))
    }

    @Test
    fun anEmptySelectionFallsBack() {
        assertEquals(FALLBACK_MIME_TYPE, commonMimeType(emptyList()))
    }

    @Test
    fun blanksAreIgnoredRatherThanWideningEverything() {
        assertEquals("image/png", commonMimeType(listOf("image/png", "")))
        assertEquals(FALLBACK_MIME_TYPE, commonMimeType(listOf("", "")))
    }
}
