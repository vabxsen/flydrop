package com.flydrop.app.ui.profile

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.profile.AvatarResult
import com.flydrop.app.data.profile.AvatarStore
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
    /** Remote failure from the last Save; shown without blocking a retry. */
    val submissionError: String? = null,
    val saving: Boolean = false,
    /** Outcome of the last attempt, shown under the field or as confirmation. */
    val message: String? = null,
    /** Colours [message] as a problem rather than a confirmation. */
    val messageIsError: Boolean = false,
    /** The user's chosen profile photo; null means the generated avatar. */
    val avatar: ImageBitmap? = null,
    /** A pick is being decoded and stored. */
    val avatarBusy: Boolean = false,
    val avatarSheetOpen: Boolean = false,
)

/**
 * Drives the editable parts of Profile: the FlyDrop ID card and the profile
 * photo.
 *
 * Uniqueness and the one-change limit on the id are decided by
 * [ProfileRepository] and the Firestore rules behind it; this only keeps the
 * editor honest about shape before a round trip is worth making, and turns the
 * repository's outcomes into something to read. The photo is [AvatarStore]'s,
 * kept on the device and filed per account.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val avatarStore = AvatarStore(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var boundUid: String? = null
    private var bound = false
    private var observeJob: Job? = null
    private var avatarJob: Job? = null

    /** Photos are filed per account; guest mode gets its own. */
    private val avatarKey: String get() = boundUid ?: AvatarStore.GUEST_KEY

    /**
     * Points the screen at an account, or at nothing when the app is running in
     * guest mode. Re-binding the same uid is a no-op, so the recompositions
     * that come with navigating back to Profile do not restart the listener.
     */
    fun bind(uid: String?) {
        if (bound && uid == boundUid) return
        bound = true
        boundUid = uid
        observeJob?.cancel()
        avatarJob?.cancel()
        _uiState.value = ProfileUiState()

        // Guest mode has no FlyDrop ID to read, but it can still have a photo.
        avatarJob = viewModelScope.launch {
            val stored = avatarStore.load(avatarKey)
            if (stored != null) _uiState.update { it.copy(avatar = stored) }
        }

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
                submissionError = null,
                message = null,
            )
        }
    }

    fun dismissEditor() {
        if (_uiState.value.saving) return
        _uiState.update {
            it.copy(
                editorOpen = false,
                input = "",
                inputError = null,
                submissionError = null,
            )
        }
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
                submissionError = null,
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
            _uiState.update {
                it.copy(inputError = error.message, submissionError = null)
            }
            return
        }

        _uiState.update {
            it.copy(
                saving = true,
                inputError = null,
                submissionError = null,
                message = null,
            )
        }
        viewModelScope.launch {
            when (val result = repository.claimFlyId(uid, handle)) {
                is ClaimResult.Success -> _uiState.update {
                    // The snapshot listener carries the new id in; this closes
                    // the editor and says so in case that listener lags.
                    it.copy(
                        saving = false,
                        editorOpen = false,
                        input = "",
                        submissionError = null,
                        flyId = result.profile.flyId,
                        canEdit = result.profile.editable && repository.isAvailable,
                        changed = result.profile.handleChanged,
                        message = "Your FlyDrop ID is now ${result.profile.flyId}.",
                    )
                }

                ClaimResult.Taken -> _uiState.update {
                    it.copy(
                        saving = false,
                        inputError = "Someone already has that id.",
                        submissionError = null,
                    )
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
                    it.copy(
                        saving = false,
                        inputError = result.error.message,
                        submissionError = null,
                    )
                }

                is ClaimResult.Failure -> _uiState.update {
                    it.copy(
                        saving = false,
                        inputError = null,
                        submissionError = result.message,
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null, messageIsError = false) }
    }

    fun openAvatarSheet() {
        if (_uiState.value.avatarBusy) return
        _uiState.update { it.copy(avatarSheetOpen = true, message = null, messageIsError = false) }
    }

    fun dismissAvatarSheet() {
        _uiState.update { it.copy(avatarSheetOpen = false) }
    }

    /**
     * Takes the photo picker's result. A null [source] means the user backed
     * out of the picker, which is not an error and leaves the current photo be.
     */
    fun onAvatarPicked(source: Uri?) {
        _uiState.update { it.copy(avatarSheetOpen = false) }
        if (source == null) return

        _uiState.update { it.copy(avatarBusy = true, message = null, messageIsError = false) }
        val key = avatarKey
        viewModelScope.launch {
            when (val result = avatarStore.save(key, source)) {
                is AvatarResult.Success -> _uiState.update {
                    it.copy(
                        avatarBusy = false,
                        avatar = result.image,
                        message = "Profile photo updated.",
                        messageIsError = false,
                    )
                }

                is AvatarResult.Failure -> _uiState.update {
                    it.copy(
                        avatarBusy = false,
                        message = result.message,
                        messageIsError = true,
                    )
                }
            }
        }
    }

    /** Drops the custom photo and goes back to the generated avatar. */
    fun removeAvatar() {
        _uiState.update {
            it.copy(
                avatarSheetOpen = false,
                avatar = null,
                message = "Back to your generated avatar.",
                messageIsError = false,
            )
        }
        val key = avatarKey
        viewModelScope.launch { avatarStore.clear(key) }
    }
}
