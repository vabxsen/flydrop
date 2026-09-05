package com.flydrop.app.ui.nearby

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.RadarDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class NearbyUiState(
    val discoverable: Boolean = true,
    val devices: List<RadarDevice> = emptyList(),
    val nearbyFriends: List<FlyUser> = emptyList(),
    val selectedUserId: String? = null,
)

/**
 * Holds nearby-discovery state.
 *
 * Real peers must come from the nearby transport when that integration is
 * active. The previous implementation animated [com.flydrop.app.data.MockData]
 * into this state, which made invented people look like devices on the phone.
 */
class NearbyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    fun setDiscoverable(discoverable: Boolean) {
        _uiState.update { state ->
            state.copy(
                discoverable = discoverable,
                devices = if (discoverable) state.devices else emptyList(),
                nearbyFriends = if (discoverable) state.nearbyFriends else emptyList(),
                selectedUserId = if (discoverable) state.selectedUserId else null,
            )
        }
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

}
