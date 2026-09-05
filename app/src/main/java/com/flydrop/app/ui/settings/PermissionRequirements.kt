package com.flydrop.app.ui.settings

import android.Manifest

/** Permission groups shown to the user instead of Android's implementation details. */
enum class AppPermission {
    Contacts,
    NearbyWifi,
    Bluetooth,
    Internet,
    InstallUpdates,
}

/**
 * Runtime permissions required by each feature on a particular Android SDK.
 *
 * Keeping this mapping outside the UI makes the version boundaries explicit
 * and testable. Internet and installing APKs are handled differently by
 * Android, so neither appears in the runtime-permission contract.
 */
@Suppress("UNUSED_PARAMETER")
internal fun runtimePermissionsFor(permission: AppPermission, sdkInt: Int): List<String> =
    when (permission) {
        AppPermission.Contacts -> listOf(Manifest.permission.READ_CONTACTS)
        // Android Quick Share owns nearby discovery. FlyDrop hands it content
        // through the Sharesheet and must not request radio/location access.
        AppPermission.NearbyWifi,
        AppPermission.Bluetooth,
        AppPermission.Internet,
        AppPermission.InstallUpdates,
        -> emptyList()
    }
