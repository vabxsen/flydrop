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
internal fun runtimePermissionsFor(permission: AppPermission, sdkInt: Int): List<String> =
    when (permission) {
        AppPermission.Contacts -> listOf(Manifest.permission.READ_CONTACTS)

        AppPermission.NearbyWifi -> if (sdkInt >= 33) {
            listOf(NEARBY_WIFI_DEVICES_PERMISSION)
        } else {
            locationPermissions
        }

        AppPermission.Bluetooth -> if (sdkInt >= 31) {
            listOf(
                BLUETOOTH_SCAN_PERMISSION,
                BLUETOOTH_CONNECT_PERMISSION,
            )
        } else {
            locationPermissions
        }

        AppPermission.Internet,
        AppPermission.InstallUpdates,
        -> emptyList()
    }

private val locationPermissions = listOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

// String literals avoid class verification of newer Manifest fields on older Android releases.
private const val NEARBY_WIFI_DEVICES_PERMISSION = "android.permission.NEARBY_WIFI_DEVICES"
private const val BLUETOOTH_SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN"
private const val BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT"
