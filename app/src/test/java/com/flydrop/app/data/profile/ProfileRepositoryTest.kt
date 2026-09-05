package com.flydrop.app.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileRepositoryTest {

    @Test
    fun offlineFailureBecomesAnActionableRetryMessage() {
        val message = profileFailureMessage(ProfileFailureReason.Connection)

        assertEquals(
            "Could not reach FlyDrop. Check your internet connection, then tap Save to retry.",
            message,
        )
        assertFalse(message.contains("client is offline", ignoreCase = true))
    }

    @Test
    fun backendConfigurationFailureDoesNotPretendThePhoneIsOffline() {
        assertEquals(
            "The FlyDrop ID service is not ready. Please try again later.",
            profileFailureMessage(ProfileFailureReason.ServiceNotReady),
        )
    }

    @Test
    fun onlyTheProfilesCurrentHandleCanBeReturnedBySearch() {
        assertEquals(true, isCurrentFlyId("current", "current"))
        assertFalse(isCurrentFlyId("retired", "current"))
        assertFalse(isCurrentFlyId("reserved", null))
    }
}
