package com.flydrop.app.ui.nearby

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.RadarDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NearbyUiState(
    val discoverable: Boolean = true,
    val devices: List<RadarDevice> = emptyList(),
    val nearbyFriends: List<FlyUser> = MockData.nearbyFriends,
    val selectedUserId: String? = MockData.ashley.id,
)

/**
 * Drives discovery. Devices arrive one at a time so the radar populates
 * gradually, which is both what the reference implies and what a real scan
 * looks like.
 */
class NearbyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        startDiscovery()
    }

    fun setDiscoverable(discoverable: Boolean) {
        _uiState.update { it.copy(discoverable = discoverable) }
        if (discoverable) startDiscovery() else stopDiscovery()
    }

    fun selectUser(userId: String?) {
        _uiState.update { it.copy(selectedUserId = userId) }
    }

    fun addFriend(user: FlyUser) {
        _uiState.update { state ->
            state.copy(
                nearbyFriends = state.nearbyFriends.map { nearbyUser ->
                    if (nearbyUser.id == user.id) nearbyUser.copy(isFriend = true) else nearbyUser
                },
                selectedUserId = user.id,
            )
        }
    }

    private fun startDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _uiState.update { it.copy(devices = emptyList()) }
            MockData.radarDevices.forEach { device ->
                delay(450)
                _uiState.update { it.copy(devices = it.devices + device) }
            }
        }
    }

    private fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _uiState.update { it.copy(devices = emptyList()) }
    }
}
