package com.flydrop.app.ui.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flydrop.app.ui.components.DialogTextButton
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/** Whether each radio discovery depends on is currently on. */
data class RadioStatus(
    val wifiEnabled: Boolean,
    val bluetoothEnabled: Boolean,
    /** False on a device with no Bluetooth hardware at all. */
    val bluetoothPresent: Boolean,
) {
    val allOn: Boolean get() = wifiEnabled && (bluetoothEnabled || !bluetoothPresent)
}

/**
 * Reads the live radio state, re-reading whenever the app comes back to the
 * foreground - which is exactly when the user returns from the Wi-Fi panel or
 * Android's Bluetooth consent dialog.
 */
@Composable
fun rememberRadioStatus(): RadioStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(context, revision) { readRadioStatus(context) }
}

/**
 * `isEnabled` on both adapters needs no runtime permission - only acting on the
 * radios does - so reading state here is always safe.
 */
internal fun readRadioStatus(context: Context): RadioStatus {
    val wifi = runCatching {
        context.applicationContext.getSystemService<WifiManager>()?.isWifiEnabled
    }.getOrNull()

    val adapter = runCatching {
        context.getSystemService<BluetoothManager>()?.adapter
    }.getOrNull()

    return RadioStatus(
        wifiEnabled = wifi == true,
        bluetoothEnabled = runCatching { adapter?.isEnabled }.getOrNull() == true,
        bluetoothPresent = adapter != null,
    )
}

/**
 * Asks the user to turn on the radios discovery needs.
 *
 * Neither switch flips the radio itself, because Android does not let an app do
 * that: `setWifiEnabled` has been a no-op since Android 10, and Bluetooth needs
 * the user's consent. Each switch instead opens the system UI that can - the
 * Wi-Fi panel, and Android's own "turn on Bluetooth?" dialog - and the rows
 * re-read their state when the app comes back.
 */
@Composable
fun NearbyRadiosDialog(
    status: RadioStatus,
    onEnableWifi: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        SoftCard(shape = FlyDrop.shapes.largeCard, elevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Turn on to find devices",
                    style = FlyDrop.type.sectionTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "FlyDrop finds people nearby over Wi-Fi and Bluetooth.",
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                )
                Spacer(Modifier.height(16.dp))

                RadioRow(
                    icon = FlyDropIcons.Wifi,
                    label = "Wi-Fi",
                    enabled = status.wifiEnabled,
                    onEnable = onEnableWifi,
                )
                if (status.bluetoothPresent) {
                    Spacer(Modifier.height(10.dp))
                    RadioRow(
                        icon = FlyDropIcons.Bluetooth,
                        label = "Bluetooth",
                        enabled = status.bluetoothEnabled,
                        onEnable = onEnableBluetooth,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    DialogTextButton(
                        label = if (status.allOn) "Done" else "Not now",
                        filled = status.allOn,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlyDrop.shapes.smallCard)
            .background(FlyDrop.colors.paleTile, FlyDrop.shapes.smallCard)
            // Tapping the row does what the switch does, so the whole strip is
            // the target rather than just the switch.
            .clickable(enabled = !enabled, onClick = onEnable)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(FlyDrop.colors.violetSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlyDrop.colors.violet,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
            )
            Text(
                text = if (enabled) "On" else "Off",
                style = FlyDrop.type.metadata,
                color = if (enabled) FlyDrop.colors.tealPressed else FlyDrop.colors.textSecondary,
            )
        }
        Switch(
            checked = enabled,
            // Only ever switched on from here. Turning a radio back off is the
            // system's job, and an app cannot do it anyway.
            onCheckedChange = { wanted -> if (wanted && !enabled) onEnable() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FlyDrop.colors.violet,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = FlyDrop.colors.textTertiary,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/**
 * Opens the slide-up Wi-Fi panel, so the user stays in FlyDrop's context.
 * Android 9 and earlier have no panel, so those fall back to Wi-Fi settings.
 */
fun openWifiControls(context: Context): Boolean {
    val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_WIFI)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }
    return context.startActivitySafely(panel) ||
        context.startActivitySafely(Intent(Settings.ACTION_WIFI_SETTINGS))
}

/** The intent Android answers with its own "turn on Bluetooth?" consent dialog. */
fun bluetoothEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

/**
 * True when [bluetoothEnableIntent] would be refused for want of
 * BLUETOOTH_CONNECT. Android 11 and earlier gate it on install-time
 * permissions instead, so nothing needs asking there.
 */
fun needsBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(
            context,
            "android.permission.BLUETOOTH_CONNECT",
        ) != PackageManager.PERMISSION_GRANTED

private fun Context.startActivitySafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NearbyRadiosDialogPreview() {
    FlyDropTheme {
        NearbyRadiosDialog(
            status = RadioStatus(
                wifiEnabled = false,
                bluetoothEnabled = true,
                bluetoothPresent = true,
            ),
            onEnableWifi = {},
            onEnableBluetooth = {},
            onDismiss = {},
        )
    }
}
