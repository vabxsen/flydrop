package com.flydrop.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.MockData
import com.flydrop.app.data.PhoneContactsRepository
import com.flydrop.app.data.model.FlyUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ContactsAccess {
    PermissionRequired,
    Requesting,
    Loading,
    Ready,
    Denied,
    Error,
}

@Immutable
data class HomeUiState(
    val currentUser: FlyUser = MockData.currentUser,
    val favouriteFriends: List<FlyUser> = emptyList(),
    val contacts: List<FlyUser> = emptyList(),
    val contactsAccess: ContactsAccess = ContactsAccess.PermissionRequired,
    val hasNotifications: Boolean = true,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val contactsRepository = PhoneContactsRepository(application.contentResolver)
    private val preferences = application.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private var favouriteContactIds = preferences
        .getStringSet(FAVOURITE_CONTACT_IDS, emptySet())
        ?.toSet()
        .orEmpty()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            contactsAccess = if (hasContactsPermission()) {
                ContactsAccess.Loading
            } else {
                ContactsAccess.PermissionRequired
            },
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        if (hasContactsPermission()) loadContacts()
    }

    fun clearNotifications() {
        _uiState.update { it.copy(hasNotifications = false) }
    }

    fun markContactsPermissionRequestStarted() {
        _uiState.update { state ->
            if (state.contactsAccess == ContactsAccess.PermissionRequired) {
                state.copy(contactsAccess = ContactsAccess.Requesting)
            } else {
                state
            }
        }
    }

    fun onContactsPermissionResult(granted: Boolean) {
        if (granted) {
            loadContacts()
        } else {
            _uiState.update { it.copy(contactsAccess = ContactsAccess.Denied) }
        }
    }

    fun requestContactsPermission() {
        _uiState.update { it.copy(contactsAccess = ContactsAccess.PermissionRequired) }
    }

    fun retryContacts() {
        if (hasContactsPermission()) loadContacts() else requestContactsPermission()
    }

    fun toggleFavourite(contact: FlyUser) {
        favouriteContactIds = toggleFavouriteId(favouriteContactIds, contact.id)
        preferences.edit {
            putStringSet(FAVOURITE_CONTACT_IDS, favouriteContactIds)
        }
        _uiState.update { state ->
            state.copy(
                favouriteFriends = favouriteContacts(state.contacts, favouriteContactIds),
            )
        }
    }

    private fun hasContactsPermission(): Boolean = ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.READ_CONTACTS,
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun loadContacts() {
        if (!hasContactsPermission()) {
            _uiState.update { it.copy(contactsAccess = ContactsAccess.Denied) }
            return
        }
        _uiState.update { it.copy(contactsAccess = ContactsAccess.Loading) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { contactsRepository.loadContacts() }
                .onSuccess { contacts ->
                    _uiState.update {
                        it.copy(
                            contacts = contacts,
                            favouriteFriends = favouriteContacts(contacts, favouriteContactIds),
                            contactsAccess = ContactsAccess.Ready,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            contactsAccess = if (error is SecurityException) {
                                ContactsAccess.Denied
                            } else {
                                ContactsAccess.Error
                            },
                        )
                    }
                }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "flydrop_home"
        const val FAVOURITE_CONTACT_IDS = "favourite_contact_ids"
    }
}

internal fun favouriteContacts(
    contacts: List<FlyUser>,
    favouriteIds: Set<String>,
): List<FlyUser> = contacts.filter { it.id in favouriteIds }

internal fun toggleFavouriteId(
    favouriteIds: Set<String>,
    contactId: String,
): Set<String> = if (contactId in favouriteIds) {
    favouriteIds - contactId
} else {
    favouriteIds + contactId
}
