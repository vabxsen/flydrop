package com.flydrop.app.data.nearby

import android.Manifest
import android.os.Build

/** Runtime grants required by the Nearby Connections transport on this SDK. */
fun nearbyRuntimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> = when {
    sdkInt >= Build.VERSION_CODES.S -> buildList {
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.BLUETOOTH_SCAN)
        if (sdkInt >= Build.VERSION_CODES.S_V2) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    sdkInt >= Build.VERSION_CODES.Q -> listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    else -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
}
