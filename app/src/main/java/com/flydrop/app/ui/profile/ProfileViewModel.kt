package com.flydrop.app.ui.profile

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.profile.ClaimResult
import com.flydrop.app.data.profile.FlyIdRules
import com.flydrop.app.data.profile.FlyProfile
import com.flydrop.app.data.profile.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(
    /** Display form, prefix included. Null until an id is known. */
    val flyId: String? = null,
    /** The change is still available: signed in, backed by Firebase, unspent. */
    val canEdit: Boolean = false,
    /** The change has been spent, so the id is now final. */
    val changed: Boolean = false,
    val editorOpen: Boolean = false,
    /** What the user has typed, without the `fly#` prefix. */
    val input: String = "",
    /** Live shape feedback while typing; blocks Save when set. */
    val inputError: String? = null,
    val saving: Boolean = false,
    /** Outcome of the last attempt, shown under the field or as confirmation. */
    val message: String? = null,
)

/**
 * Drives the FlyDrop ID card on Profile.
 *
 * Uniqueness and the one-change limit are decided by [ProfileRepository] and the
 * Firestore rules behind it; this only keeps the editor honest about shape
 * before a round trip is worth making, and turns the repository's outcomes into
 * something to read.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var boundUid: String? = null
    private var observeJob: Job? = null

    /**
     * Points the screen at an account, or at nothing when the app is running in
     * guest mode. Re-binding the same uid is a no-op, so the recompositions
     * that come with navigating back to Profile do not restart the listener.
     */
    fun bind(uid: String?) {
        if (uid == boundUid) return
        boundUid = uid
        observeJob?.cancel()
        _uiState.value = ProfileUiState()

        val account = uid ?: return
        observeJob = viewModelScope.launch {
            // Reserves the derived default id if this account has never been
            // seen, so nobody can claim the id the user is already showing.
            // Its result seeds the card while the listener is still connecting.
            show(repository.ensureProfile(account))
            repository.observeProfile(account).collect(::show)
        }
    }

    private fun show(profile: FlyProfile) {
        _uiState.update { state ->
            state.copy(
                flyId = profile.flyId,
                canEdit = profile.editable && repository.isAvailable,
                changed = profile.handleChanged,
            )
        }
    }

    fun openEditor() {
        val state = _uiState.value
        if (!state.canEdit || state.editorOpen) return
        _uiState.update {
            it.copy(
                editorOpen = true,
                // Start empty rather than pre-filled: the derived default is
                // not something anyone wants to edit character by character.
                input = "",
                inputError = null,
                message = null,
            )
        }
    }

    fun dismissEditor() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(editorOpen = false, input = "", inputError = null) }
    }

    fun onInputChange(value: String) {
        val normalised = FlyIdRules.normalise(value).take(FlyIdRules.MAX_LENGTH)
        _uiState.update {
            it.copy(
                input = normalised,
                // Nothing typed yet is not yet an error to complain about.
                inputError = if (normalised.isEmpty()) {
                    null
                } else {
                    FlyIdRules.validate(normalised)?.message
                },
                message = null,
            )
        }
    }

    fun confirmEdit() {
        val uid = boundUid ?: return
        val state = _uiState.value
        if (state.saving || !state.canEdit) return

        val handle = FlyIdRules.normalise(state.input)
        FlyIdRules.validate(handle)?.let { error ->
            _uiState.update { it.copy(inputError = error.message) }
            return
        }

        _uiState.update { it.copy(saving = true, inputError = null, message = null) }
        viewModelScope.launch {
            when (val result = repository.claimFlyId(uid, handle)) {
                is ClaimResult.Success -> _uiState.update {
                    // The snapshot listener carries the new id in; this closes
                    // the editor and says so in case that listener lags.
                    it.copy(
                        saving = false,
                        editorOpen = false,
                        input = "",
                        flyId = result.profile.flyId,
                        canEdit = result.profile.editable && repository.isAvailable,
                        changed = result.profile.handleChanged,
                        message = "Your FlyDrop ID is now ${result.profile.flyId}.",
                    )
                }

                ClaimResult.Taken -> _uiState.update {
                    it.copy(saving = false, inputError = "Someone already has that id.")
                }

                ClaimResult.AlreadyChanged -> _uiState.update {
                    it.copy(
                        saving = false,
                        editorOpen = false,
                        canEdit = false,
                        changed = true,
                        message = "Your FlyDrop ID has already been changed once.",
                    )
                }

                is ClaimResult.Invalid -> _uiState.update {
                    it.copy(saving = false, inputError = result.error.message)
                }

                is ClaimResult.Failure -> _uiState.update {
                    it.copy(saving = false, inputError = result.message)
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
