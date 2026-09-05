package com.flydrop.app.ui.nearby

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyViewModelTest {

    @Test
    fun initialStateDoesNotInventNearbyPeopleOrDevices() {
        val state = NearbyViewModel().uiState.value

        assertTrue(state.devices.isEmpty())
        assertTrue(state.nearbyFriends.isEmpty())
        assertNull(state.selectedUserId)
    }

    @Test
    fun togglingDiscoveryDoesNotPopulateMockPeople() {
        val viewModel = NearbyViewModel()

        viewModel.setDiscoverable(false)
        assertFalse(viewModel.uiState.value.discoverable)
        viewModel.setDiscoverable(true)

        val state = viewModel.uiState.value
        assertTrue(state.discoverable)
        assertTrue(state.devices.isEmpty())
        assertTrue(state.nearbyFriends.isEmpty())
    }
}
