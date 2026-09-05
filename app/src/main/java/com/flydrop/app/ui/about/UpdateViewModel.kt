package com.flydrop.app.ui.about

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.update.AppRelease
import com.flydrop.app.data.update.DownloadResult
import com.flydrop.app.data.update.UpdateCheck
import com.flydrop.app.data.update.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateStatus {
    /** Nothing asked for yet. */
    Idle,
    Checking,
    UpToDate,
    Available,
    Downloading,

    /** Downloaded and verified; the system installer has been offered it. */
    ReadyToInstall,

    /** The user has not allowed this app to install others yet. */
    PermissionNeeded,
    Failed,
}

@Immutable
data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.Idle,
    val release: AppRelease? = null,
    /** 0..1 while downloading, or null when the size is unknown. */
    val progress: Float? = null,
    val message: String? = null,
    val apk: File? = null,
) {
    val busy: Boolean get() = status == UpdateStatus.Checking || status == UpdateStatus.Downloading
}

/**
 * Drives About's update card: ask GitHub what the latest release is, and if it
 * is newer than this build, fetch and install it.
 *
 * Deciding whether a release is newer belongs to
 * [com.flydrop.app.data.update.AppVersion], and downloading and verifying it to
 * [UpdateRepository]. This only sequences those and keeps the screen honest
 * about which step it is on.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(application)

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    fun check(apiUrl: String, installedVersion: String) {
        if (_uiState.value.busy) return
        _uiState.value = UpdateUiState(status = UpdateStatus.Checking)
        job = viewModelScope.launch {
            when (val result = repository.check(apiUrl, installedVersion)) {
                is UpdateCheck.Available -> _uiState.value = UpdateUiState(
                    status = UpdateStatus.Available,
                    release = result.release,
                )

                is UpdateCheck.UpToDate -> _uiState.value = UpdateUiState(
                    status = UpdateStatus.UpToDate,
                    message = "You are on the latest version (${result.installed}).",
                )

                UpdateCheck.NoReleases -> _uiState.value = UpdateUiState(
                    status = UpdateStatus.UpToDate,
                    message = "No releases have been published yet.",
                )

                is UpdateCheck.Failed -> _uiState.value = UpdateUiState(
                    status = UpdateStatus.Failed,
                    message = result.message,
                )
            }
        }
    }

    /**
     * Downloads the offered release and hands it to the system installer.
     *
     * The permission is checked first: without it the install can never be
     * offered, and downloading several megabytes to then say so would waste the
     * user's data.
     */
    fun downloadAndInstall() {
        val state = _uiState.value
        val release = state.release ?: return
        if (state.busy) return

        // Defence in depth: eligibility normally rejects this before it can
        // reach the UI, but never request a sensitive setting without an APK.
        if (release.apkUrl.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    status = UpdateStatus.Failed,
                    message = "That release has no APK to install.",
                )
            }
            return
        }

        if (!repository.canInstallPackages()) {
            _uiState.update {
                it.copy(
                    status = UpdateStatus.PermissionNeeded,
                    message = "Android needs your permission for FlyDrop to install updates.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(status = UpdateStatus.Downloading, progress = 0f, message = null)
        }
        job = viewModelScope.launch {
            val result = repository.download(release) { fraction ->
                _uiState.update { it.copy(progress = fraction) }
            }
            when (result) {
                is DownloadResult.Success -> {
                    val launched = repository.install(result.apk)
                    _uiState.update {
                        it.copy(
                            status = UpdateStatus.ReadyToInstall,
                            progress = null,
                            apk = result.apk,
                            message = if (launched) {
                                null
                            } else {
                                "Downloaded, but the installer could not be opened."
                            },
                        )
                    }
                }

                is DownloadResult.Failed -> _uiState.update {
                    it.copy(
                        status = UpdateStatus.Failed,
                        progress = null,
                        message = result.message,
                    )
                }
            }
        }
    }

    /** Re-opens the installer for an update already downloaded and verified. */
    fun install() {
        val apk = _uiState.value.apk ?: return
        if (!repository.install(apk)) {
            _uiState.update { it.copy(message = "The installer could not be opened.") }
        }
    }

    fun openInstallPermissionSettings() {
        if (repository.openInstallPermissionSettings()) {
            // Coming back from Settings, the offer is still standing and the
            // permission may now be granted, so put the user back on the button
            // rather than on the explanation they have just acted upon.
            _uiState.update { it.copy(status = UpdateStatus.Available, message = null) }
        } else {
            _uiState.update {
                it.copy(message = "Could not open the install-permission screen.")
            }
        }
    }

    fun dismiss() {
        job?.cancel()
        _uiState.value = UpdateUiState()
    }
}
