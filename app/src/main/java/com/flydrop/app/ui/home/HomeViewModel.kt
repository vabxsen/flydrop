package com.flydrop.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.ActivityEntry
import com.flydrop.app.data.model.FlyUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class HomeUiState(
    val currentUser: FlyUser = MockData.currentUser,
    val favouriteFriends: List<FlyUser> = MockData.favouriteFriends,
    val activities: List<ActivityEntry> = MockData.activities,
    val hasNotifications: Boolean = true,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun clearNotifications() {
        _uiState.update { it.copy(hasNotifications = false) }
    }
}
