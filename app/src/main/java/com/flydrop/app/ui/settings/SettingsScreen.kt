package com.flydrop.app.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

enum class PermissionAccess {
    Granted,
    Required,

    /**
     * Denied firmly enough that Android will no longer show the request
     * dialog. Asking again is a no-op, so the only way forward is Settings.
     */
    Blocked,
    BuiltIn,
}

data class PermissionItem(
    val permission: AppPermission,
    val title: String,
    val description: String,
    val access: PermissionAccess,
)

/**
 * Stateful entry point for Settings. Permission status is refreshed after a
 * runtime request and whenever the user returns from Android's Settings app.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var actionError by remember { mutableStateOf<String?>(null) }

    // Android never reports "permanently denied" directly. What it does report
    // is that a rationale should no longer be shown - which, straight after a
    // denial, means the dialog will not appear again. Recorded here so the row
    // can send the user to Settings instead of re-requesting into silence.
    var blocked by remember { mutableStateOf(emptySet<AppPermission>()) }
    var requested by remember { mutableStateOf<AppPermission?>(null) }
    val activity = remember(context) { context.findActivity() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        requested?.let { permission ->
            blocked = if (results.isPermanentDenial(activity)) {
                blocked + permission
            } else {
                blocked - permission
            }
        }
        requested = null
        revision++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val items = remember(context, revision, blocked) {
        permissionItems(context = context, sdkInt = Build.VERSION.SDK_INT, blocked = blocked)
    }

    fun openAppSettings(): Boolean = openSettingsIntent(
        context = context,
        intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri(),
        ),
    )

    fun openInstallSettings(): Boolean = openSettingsIntent(
        context = context,
        intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ),
    )

    SettingsScreen(
        items = items,
        onBack = onBack,
        onPermissionClick = { item ->
            actionError = null
            when {
                item.permission == AppPermission.InstallUpdates -> {
                    if (!openInstallSettings()) {
                        actionError = "Android could not open the install-permission settings."
                    }
                }

                item.access == PermissionAccess.Granted ||
                    item.access == PermissionAccess.Blocked -> {
                    if (!openAppSettings()) {
                        actionError = "Android could not open FlyDrop's app settings."
                    }
                }

                item.access == PermissionAccess.Required -> {
                    requested = item.permission
                    permissionLauncher.launch(
                        runtimePermissionsFor(item.permission, Build.VERSION.SDK_INT).toTypedArray(),
                    )
                }
            }
        },
        onOpenAndroidSettings = {
            actionError = if (openAppSettings()) {
                null
            } else {
                "Android could not open FlyDrop's app settings."
            }
        },
        actionError = actionError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    items: List<PermissionItem>,
    onBack: () -> Unit,
    onPermissionClick: (PermissionItem) -> Unit,
    onOpenAndroidSettings: () -> Unit,
    modifier: Modifier = Modifier,
    actionError: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlyDrop.colors.heroAqua),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            SettingsTopBar(onBack = onBack)
            Spacer(Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(FlyDrop.shapes.sheet)
                .background(FlyDrop.colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlyDrop.dimens.screenPadding)
                .padding(
                    top = FlyDrop.dimens.panelTopPadding,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            Text(
                text = "Permissions",
                style = FlyDrop.type.sectionTitle,
                color = FlyDrop.colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Control what FlyDrop can access on this phone.",
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            items.forEachIndexed { index, item ->
                PermissionCard(
                    item = item,
                    onClick = { onPermissionClick(item) },
                )
                if (index != items.lastIndex) Spacer(Modifier.height(FlyDrop.dimens.cardGap))
            }

            Spacer(Modifier.height(20.dp))
            PermissionNote(onOpenAndroidSettings = onOpenAndroidSettings)

            if (actionError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = actionError,
                    style = FlyDrop.type.metadata,
                    color = ErrorRed,
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FlyDrop.dimens.topBarHeight)
            .padding(horizontal = FlyDrop.dimens.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Settings",
            style = FlyDrop.type.screenTitle,
            color = FlyDrop.colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FlyDropIcons.ArrowLeft,
                contentDescription = "Back",
                tint = FlyDrop.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (item.permission) {
        AppPermission.Contacts -> FlyDropIcons.Contacts
        AppPermission.NearbyWifi -> FlyDropIcons.Wifi
        AppPermission.Bluetooth -> FlyDropIcons.Bluetooth
        AppPermission.Internet -> FlyDropIcons.Globe
        AppPermission.InstallUpdates -> FlyDropIcons.Download
    }
    val statusLabel = when (item.access) {
        PermissionAccess.Granted -> "Allowed"
        PermissionAccess.Required -> "Permission needed"
        PermissionAccess.Blocked -> "Blocked - change it in Android settings"
        PermissionAccess.BuiltIn -> "Available"
    }
    val showAction = item.access != PermissionAccess.BuiltIn
    val actionLabel = when {
        item.permission == AppPermission.InstallUpdates -> "Manage"
        item.access == PermissionAccess.Granted -> "Manage"
        // Requesting again would be silently ignored, so send them where the
        // decision can actually be reversed.
        item.access == PermissionAccess.Blocked -> "Settings"
        else -> "Allow"
    }

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        // The whole row acts as the button too, so a tap that lands beside the
        // pill still does what the user meant.
        onClick = if (showAction) onClick else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(FlyDrop.colors.violetSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FlyDrop.colors.violet,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = FlyDrop.type.metadata,
                    color = FlyDrop.colors.textSecondary,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = statusLabel,
                    style = FlyDrop.type.metadata,
                    color = when (item.access) {
                        PermissionAccess.Required -> FlyDrop.colors.violet
                        PermissionAccess.Blocked -> ErrorRed
                        else -> FlyDrop.colors.tealPressed
                    },
                )
            }
            if (showAction) {
                Spacer(Modifier.width(10.dp))
                Box(
                    // 48dp is Android's minimum touch target. The pill used to
                    // be 35x19dp, small enough that ordinary taps missed it and
                    // the button read as doing nothing.
                    modifier = Modifier
                        .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
                        .clip(FlyDrop.shapes.button)
                        .background(FlyDrop.colors.violetSoft)
                        .clickable(onClick = onClick)
                        .semantics { role = Role.Button }
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = actionLabel,
                        style = FlyDrop.type.buttonLabel,
                        color = FlyDrop.colors.violet,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionNote(
    onOpenAndroidSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlyDrop.shapes.smallCard)
            .background(FlyDrop.colors.tealSoft)
            .padding(14.dp),
    ) {
        Text(
            text = "Your privacy stays in your hands",
            style = FlyDrop.type.cardTitle,
            color = FlyDrop.colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "FlyDrop asks for contacts only when you choose to show them. " +
                "Bluetooth and Nearby Wi-Fi are requested only when you start direct FlyDrop " +
                "sharing. Quick Share keeps managing its own visibility.",
            style = FlyDrop.type.metadata,
            color = FlyDrop.colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Open Android settings",
            style = FlyDrop.type.buttonLabel,
            color = FlyDrop.colors.violet,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(FlyDrop.shapes.button)
                .clickable(onClick = onOpenAndroidSettings)
                .semantics { role = Role.Button }
                .padding(vertical = 7.dp, horizontal = 3.dp),
        )
    }
}

internal fun permissionItems(
    context: Context,
    sdkInt: Int,
    blocked: Set<AppPermission> = emptySet(),
): List<PermissionItem> = listOf(
    PermissionItem(
        permission = AppPermission.Contacts,
        title = "Contacts",
        description = "Show contact names in your FlyDrop list.",
        access = runtimeAccess(context, AppPermission.Contacts, sdkInt, blocked),
    ),
    PermissionItem(
        permission = AppPermission.NearbyWifi,
        title = "Nearby Wi-Fi",
        description = "Used to transfer directly between nearby FlyDrop devices.",
        access = if (sdkInt >= Build.VERSION_CODES.S_V2) {
            runtimeAccess(context, AppPermission.NearbyWifi, sdkInt, blocked)
        } else {
            PermissionAccess.BuiltIn
        },
    ),
    PermissionItem(
        permission = AppPermission.Bluetooth,
        title = "Bluetooth",
        description = "Used to find and connect to nearby FlyDrop devices.",
        access = runtimeAccess(context, AppPermission.Bluetooth, sdkInt, blocked),
    ),
    PermissionItem(
        permission = AppPermission.Internet,
        title = "Internet access",
        description = "Sign in, sync your profile, and check for updates.",
        access = PermissionAccess.BuiltIn,
    ),
    PermissionItem(
        permission = AppPermission.InstallUpdates,
        title = "Install updates",
        description = "Allow FlyDrop to install downloaded APK updates.",
        access = if (context.packageManager.canRequestPackageInstalls()) {
            PermissionAccess.Granted
        } else {
            PermissionAccess.Required
        },
    ),
)

private fun runtimeAccess(
    context: Context,
    permission: AppPermission,
    sdkInt: Int,
    blocked: Set<AppPermission>,
): PermissionAccess {
    val granted = runtimePermissionsFor(permission, sdkInt).all { androidPermission ->
        ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
    }
    return accessFor(granted = granted, blocked = permission in blocked)
}

/**
 * A live grant outranks a block recorded earlier in the session: the user can
 * reverse a block from Android's settings while this screen is backgrounded,
 * and the row must not keep claiming the permission is unavailable.
 */
internal fun accessFor(granted: Boolean, blocked: Boolean): PermissionAccess = when {
    granted -> PermissionAccess.Granted
    blocked -> PermissionAccess.Blocked
    else -> PermissionAccess.Required
}

/**
 * True when a denial will not be re-promptable.
 *
 * Read straight after a request: Android shows a rationale after the first
 * refusal and stops showing one after the refusal that makes the decision
 * final, so a denied permission with no rationale left means the dialog is
 * spent. Without an Activity the flag cannot be read, so this reports false
 * and the row keeps offering to ask.
 */
private fun Map<String, Boolean>.isPermanentDenial(activity: Activity?): Boolean {
    if (activity == null) return false
    return any { (permission, granted) ->
        !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openSettingsIntent(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

private val ErrorRed = Color(0xFFD1453B)

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun SettingsScreenPreview() {
    FlyDropTheme {
        SettingsScreen(
            items = listOf(
                PermissionItem(
                    AppPermission.Contacts,
                    "Contacts",
                    "Show contact names in your FlyDrop list.",
                    PermissionAccess.Granted,
                ),
                PermissionItem(
                    AppPermission.NearbyWifi,
                    "Nearby Wi-Fi",
                    "Find and connect to nearby FlyDrop devices.",
                    PermissionAccess.Required,
                ),
                PermissionItem(
                    AppPermission.Bluetooth,
                    "Bluetooth",
                    "Discover nearby devices and make connections.",
                    PermissionAccess.Blocked,
                ),
                PermissionItem(
                    AppPermission.Internet,
                    "Internet access",
                    "Sign in, sync your profile, and check for updates.",
                    PermissionAccess.BuiltIn,
                ),
                PermissionItem(
                    AppPermission.InstallUpdates,
                    "Install updates",
                    "Allow FlyDrop to install downloaded APK updates.",
                    PermissionAccess.Required,
                ),
            ),
            onBack = {},
            onPermissionClick = {},
            onOpenAndroidSettings = {},
        )
    }
}
