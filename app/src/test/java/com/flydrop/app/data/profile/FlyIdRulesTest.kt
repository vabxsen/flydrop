package com.flydrop.app.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlyIdRulesTest {

    @Test
    fun normalisationFoldsCaseAndStripsThePrefix() {
        assertEquals("lucas", FlyIdRules.normalise("Lucas"))
        assertEquals("lucas", FlyIdRules.normalise("fly#Lucas"))
        assertEquals("lucas", FlyIdRules.normalise("FLY#LUCAS"))
        assertEquals("lucas", FlyIdRules.normalise("#lucas"))
        assertEquals("lucas", FlyIdRules.normalise("  fly# lucas  "))
    }

    /** Case folding is what stops two accounts holding one id in two spellings. */
    @Test
    fun spellingsThatDifferOnlyInCaseNormaliseToOneHandle() {
        assertEquals(FlyIdRules.normalise("Lucas_S"), FlyIdRules.normalise("lUCAS_s"))
    }

    @Test
    fun wellFormedHandlesAreAccepted() {
        listOf("abc", "lucas", "lucas_s", "lucas.s", "a1b2c3", "l".repeat(20))
            .forEach { assertNull("expected $it to be valid", FlyIdRules.validate(it)) }
    }

    @Test
    fun lengthIsBounded() {
        assertEquals(FlyIdError.TooShort, FlyIdRules.validate("ab"))
        assertEquals(FlyIdError.TooLong, FlyIdRules.validate("l".repeat(21)))
    }

    @Test
    fun separatorsMayNotLeadTrailOrDouble() {
        listOf("_lucas", "lucas_", ".lucas", "lucas.", "lucas__s", "lucas._s")
            .forEach { assertEquals("expected $it to be rejected", FlyIdError.BadShape, FlyIdRules.validate(it)) }
    }

    @Test
    fun charactersOutsideTheAllowedSetAreRejected() {
        listOf("lucas s", "lucas-s", "lucas!", "lucas#s", "lucås")
            .forEach { assertEquals("expected $it to be rejected", FlyIdError.BadShape, FlyIdRules.validate(it)) }
    }

    @Test
    fun reservedHandlesAreHeldBack() {
        assertEquals(FlyIdError.Reserved, FlyIdRules.validate("flydrop"))
        assertEquals(FlyIdError.Reserved, FlyIdRules.validate("support"))
        assertEquals(FlyIdError.Reserved, FlyIdRules.validate(FlyIdRules.normalise("Admin")))
    }

    @Test
    fun displayCarriesThePrefix() {
        assertEquals("fly#lucas", FlyIdRules.display("lucas"))
        assertEquals("lucas", FlyIdRules.normalise(FlyIdRules.display("lucas")))
    }

    /** The derived default is written straight to Firestore, so it must pass validation. */
    @Test
    fun derivedDefaultsAreAlwaysValid() {
        listOf(
            "Ab3xY9QwErTy1234567890",
            "1",
            "--",
            "",
            "u_i-d.WITH/punctuation",
        ).forEach { uid ->
            repeat(8) { attempt ->
                val handle = FlyIdRules.defaultHandle(uid, attempt)
                assertNull("expected $handle (uid=$uid) to be valid", FlyIdRules.validate(handle))
            }
        }
    }

    @Test
    fun derivedDefaultsAreStableAndDisambiguatedByAttempt() {
        val uid = "abc123XYZ"
        assertEquals(FlyIdRules.defaultHandle(uid), FlyIdRules.defaultHandle(uid))
        val candidates = (0 until 8).map { FlyIdRules.defaultHandle(uid, it) }
        assertEquals(candidates.size, candidates.toSet().size)
    }

    @Test
    fun everyErrorCarriesSomethingToShowTheUser() {
        FlyIdError.entries.forEach { error ->
            assertNotNull(error.message)
            assertTrue(error.message.isNotBlank())
        }
    }
}
