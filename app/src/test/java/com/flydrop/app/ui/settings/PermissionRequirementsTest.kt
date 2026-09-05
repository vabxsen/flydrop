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
    fun quickShareWifiDoesNotRequestAppRuntimePermission() {
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.NearbyWifi, 26))
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.NearbyWifi, 36))
    }

    @Test
    fun quickShareBluetoothDoesNotRequestAppRuntimePermission() {
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.Bluetooth, 26))
        assertEquals(emptyList<String>(), runtimePermissionsFor(AppPermission.Bluetooth, 36))
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
