package com.flydrop.app.ui.settings

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRequirementsTest {

    @Test
    fun contactsAlwaysUsesReadContacts() {
        assertEquals(
            listOf(Manifest.permission.READ_CONTACTS),
            runtimePermissionsFor(AppPermission.Contacts, sdkInt = 26),
        )
        assertEquals(
            listOf(Manifest.permission.READ_CONTACTS),
            runtimePermissionsFor(AppPermission.Contacts, sdkInt = 36),
        )
    }

    @Test
    fun nearbyWifiUsesTheAndroid12PermissionWhenAvailable() {
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.NearbyWifi, 26))
        assertEquals(
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES),
            runtimePermissionsFor(AppPermission.NearbyWifi, 36),
        )
    }

    @Test
    fun bluetoothUsesTheRequiredNearbyRuntimePermissionForEachPlatform() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            runtimePermissionsFor(AppPermission.Bluetooth, 26),
        )
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            runtimePermissionsFor(AppPermission.Bluetooth, 29),
        )
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            ),
            runtimePermissionsFor(AppPermission.Bluetooth, 36),
        )
    }

    @Test
    fun specialAndNormalPermissionsDoNotUseRuntimeDialog() {
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.Internet, 36))
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.InstallUpdates, 36))
    }

    @Test
    fun aDeniedPermissionAndroidWillNotRePromptForOffersSettings() {
        assertEquals(
            PermissionAccess.Blocked,
            accessFor(granted = false, blocked = true),
        )
        assertEquals(
            PermissionAccess.Required,
            accessFor(granted = false, blocked = false),
        )
    }

    @Test
    fun grantingFromAndroidSettingsClearsAnEarlierBlock() {
        assertEquals(
            PermissionAccess.Granted,
            accessFor(granted = true, blocked = true),
        )
    }
}
