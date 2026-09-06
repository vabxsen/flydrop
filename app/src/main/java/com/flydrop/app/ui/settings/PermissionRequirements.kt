package com.flydrop.app.ui.settings

import android.Manifest
import android.os.Build
import com.flydrop.app.data.nearby.nearbyRuntimePermissions

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
internal fun runtimePermissionsFor(permission: AppPermission, sdkInt: Int): List<String> =
    when (permission) {
        AppPermission.Contacts -> listOf(Manifest.permission.READ_CONTACTS)
        AppPermission.NearbyWifi -> if (sdkInt >= Build.VERSION_CODES.S_V2) {
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            emptyList()
        }

        AppPermission.Bluetooth -> nearbyRuntimePermissions(sdkInt).filterNot {
            it == Manifest.permission.NEARBY_WIFI_DEVICES
        }

        AppPermission.Internet,
        AppPermission.InstallUpdates,
        -> emptyList()
    }
