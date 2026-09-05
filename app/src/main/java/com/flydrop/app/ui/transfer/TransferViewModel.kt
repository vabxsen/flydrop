package com.flydrop.app.ui.transfer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.TransferState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TransferUiState(
    val transfer: TransferState = MockData.transfer,
)

/**
 * Advances the mock transfer so the arc and the per-file rings animate against
 * real state rather than a hard-coded value.
 */
class TransferViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransferUiState(transfer = MockData.transfer.copy(progress = 0f)),
    )
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(400)
            // Settle at the value shown in the reference rather than completing.
            _uiState.update { it.copy(transfer = it.transfer.copy(progress = 0.70f)) }
        }
    }
}
