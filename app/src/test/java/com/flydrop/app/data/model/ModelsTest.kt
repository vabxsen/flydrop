package com.flydrop.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {

    @Test
    fun transferProgressIsCalculatedAndClamped() {
        assertEquals(0.5f, file(total = 200, transferred = 100).progress!!, 0.0001f)
        assertEquals(1f, file(total = 200, transferred = 300).progress!!, 0.0001f)
        assertEquals(0f, file(total = 0, transferred = 100).progress!!, 0.0001f)
    }

    @Test
    fun queuedFileHasNoProgress() {
        assertNull(file(total = 200, transferred = null).progress)
    }

    private fun file(total: Long, transferred: Long?) = TransferFile(
        id = "test",
        name = "test.bin",
        kind = FileKind.Document,
        totalBytes = total,
        transferredBytes = transferred,
    )
}
