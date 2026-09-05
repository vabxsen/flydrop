package com.flydrop.app.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.flydrop.app.data.update.AppRelease
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * What About reports. Passed in rather than read from `BuildConfig` and
 * `Build` here, so the screen stays previewable and does not depend on the
 * build or the device it is running on.
 *
 * [androidRelease], [sdkInt] and [device] are not shown; they go into the
 * prefilled bug report, so an issue arrives with the context a maintainer would
 * otherwise have to ask for.
 */
data class AboutInfo(
    val versionName: String,
    val versionCode: Int,
    val packageName: String,
    val debugBuild: Boolean,
    val sourceUrl: String,
    val androidRelease: String,
    val sdkInt: Int,
    val device: String,
) {
    val issuesUrl: String get() = "$sourceUrl/issues"

    /**
     * A GitHub "new issue" URL carrying a template and the build it came from.
     * GitHub fills the form from the `body` parameter, so the reporter lands on
     * a part-written report rather than an empty box.
     *
     * Encoded with [URLEncoder] rather than `Uri.encode` so this stays plain
     * Kotlin and can be covered by a unit test.
     */
    val newIssueUrl: String
        get() {
            val body = """
                **What happened?**


                **What did you expect?**


                **Steps to reproduce**
                1.
                2.

                ---
                FlyDrop $versionName (build $versionCode) - ${if (debugBuild) "debug" else "release"}
                Android $androidRelease (API $sdkInt) - $device
            """.trimIndent()
            return "$issuesUrl/new?body=" + URLEncoder.encode(body, "UTF-8")
        }
}

enum class AboutTab(val label: String) {
    Version("Version"),
    Credits("Credits"),
}

/**
 * About, reached from Profile.
 *
 * Two tabs over one scrolling sheet: what this build is, and what it was built
 * out of. Version also carries the two ways out to the project - the source and
 * a prefilled bug report. The selected tab survives configuration changes, so a
 * rotation does not quietly send the reader back to Version.
 */
@Composable
fun AboutScreen(
    info: AboutInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    updateState: UpdateUiState = UpdateUiState(),
    onCheckForUpdate: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onGrantInstallPermission: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AboutTab.Version) }
    var linkError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // A device with no browser cannot open GitHub. Rare, but the alternative is
    // a tap that does nothing at all and looks like a broken button.
    val openLink: (String) -> Unit = { url ->
        linkError = try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            null
        } catch (_: ActivityNotFoundException) {
            "No app on this device can open links."
        }
    }

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
            AboutTopBar(onBack = onBack)
            Spacer(Modifier.height(4.dp))
            AboutTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = FlyDrop.dimens.screenPadding),
            )
            Spacer(Modifier.height(20.dp))
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
            when (selectedTab) {
                AboutTab.Version -> VersionTab(
                    info = info,
                    updateState = updateState,
                    onOpenLink = openLink,
                    onCheckForUpdate = onCheckForUpdate,
                    onDownloadUpdate = onDownloadUpdate,
                    onInstallUpdate = onInstallUpdate,
                    onGrantInstallPermission = onGrantInstallPermission,
                )
                AboutTab.Credits -> CreditsTab(info = info)
            }

            if (linkError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = linkError.orEmpty(),
                    style = FlyDrop.type.metadata,
                    color = ErrorRed,
                )
            }
        }
    }
}

/**
 * Back affordance on the left with the title optically centred, matching the
 * File Transfer screen so the two detail screens read as the same kind of page.
 */
@Composable
private fun AboutTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FlyDrop.dimens.topBarHeight)
            .padding(horizontal = FlyDrop.dimens.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "About",
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

/**
 * The same pale-container-with-violet-pill treatment as the bottom navigation,
 * so a second selection control does not introduce a second visual language.
 * Material's TabRow draws an underline indicator and cannot produce this.
 */
@Composable
private fun AboutTabs(
    selected: AboutTab,
    onSelect: (AboutTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FlyDrop.shapes.navContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(FlyDrop.colors.navContainer, shape)
            .padding(FlyDrop.dimens.navInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AboutTab.entries.forEach { tab ->
            AboutTabItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun RowScope.AboutTabItem(
    tab: AboutTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) FlyDrop.colors.violet else Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "aboutTabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else FlyDrop.colors.iconMuted,
        animationSpec = tween(durationMillis = 240),
        label = "aboutTabContent",
    )
    val shape = FlyDrop.shapes.navPill

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(containerColor, shape)
            .clickable(onClick = onClick)
            // Merged rather than cleared, so the node keeps the label from the
            // Text inside it and adds the tab role and selected state a screen
            // reader needs to announce it as one of two choices.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.label,
            style = FlyDrop.type.buttonLabel,
            color = contentColor,
        )
    }
}

@Composable
private fun VersionTab(
    info: AboutInfo,
    updateState: UpdateUiState,
    onOpenLink: (String) -> Unit,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onGrantInstallPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlyDropLogo()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Version ${info.versionName}",
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textSecondary,
            )
            Spacer(Modifier.height(22.dp))
        }

        DetailCard("Version", info.versionName)
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        DetailCard("Build", info.versionCode.toString())
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        DetailCard("Package", info.packageName)
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        DetailCard("Build type", if (info.debugBuild) "Debug" else "Release")

        Spacer(Modifier.height(FlyDrop.dimens.sectionGap))
        UpdateCard(
            state = updateState,
            onCheck = onCheckForUpdate,
            onDownload = onDownloadUpdate,
            onInstall = onInstallUpdate,
            onGrantPermission = onGrantInstallPermission,
            onOpenNotes = { updateState.release?.pageUrl?.let(onOpenLink) },
        )

        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        LinkCard(
            icon = FlyDropIcons.Code,
            title = "Source code",
            subtitle = "View FlyDrop on GitHub",
            onClick = { onOpenLink(info.sourceUrl) },
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        LinkCard(
            icon = FlyDropIcons.Bug,
            title = "Report a bug",
            subtitle = "Open an issue, prefilled with this build",
            onClick = { onOpenLink(info.newIssueUrl) },
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Peer-to-peer transfer is not wired up yet; nearby devices " +
                "and transfers in this build use sample data.",
            style = FlyDrop.type.metadata,
            color = FlyDrop.colors.textTertiary,
        )
    }
}

/**
 * The update check, and everything that follows from it, in one card.
 *
 * It stays a single card through every step rather than swapping in new rows,
 * so the thing the user tapped is the thing that answers them. The download is
 * never started for them: an update is several megabytes and an install prompt,
 * so it waits for a second, deliberate tap.
 */
@Composable
private fun UpdateCard(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    val idle = state.status == UpdateStatus.Idle

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        // Only a card that does nothing yet is itself the button; once it has
        // something to say, the actions inside it take over.
        onClick = if (idle) onCheck else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(FlyDrop.shapes.tile)
                        .background(colors.violetSoft, FlyDrop.shapes.tile),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            color = colors.violet,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(17.dp),
                        )
                    } else {
                        Icon(
                            imageVector = FlyDropIcons.Download,
                            contentDescription = null,
                            tint = colors.violet,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check for update",
                        style = FlyDrop.type.cardTitle,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.subtitle(),
                        style = FlyDrop.type.metadata,
                        color = if (state.status == UpdateStatus.Failed) {
                            ErrorRed
                        } else {
                            colors.textSecondary
                        },
                    )
                }
                if (idle) {
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = FlyDropIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (state.status == UpdateStatus.Downloading) {
                Spacer(Modifier.height(12.dp))
                DownloadProgress(progress = state.progress)
            }

            val release = state.release
            if (state.status == UpdateStatus.Available && release != null &&
                release.notes.isNotBlank()
            ) {
                Spacer(Modifier.height(10.dp))
                Text(
                    // Release notes are arbitrary markdown from GitHub, so they
                    // are shown as plain text and trimmed rather than rendered.
                    text = release.notes.lineSequence().take(NOTE_LINES).joinToString("\n"),
                    style = FlyDrop.type.metadata,
                    color = colors.textSecondary,
                )
            }

            UpdateActions(
                state = state,
                onCheck = onCheck,
                onDownload = onDownload,
                onInstall = onInstall,
                onGrantPermission = onGrantPermission,
                onOpenNotes = onOpenNotes,
            )
        }
    }
}

@Composable
private fun UpdateActions(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    val actions: List<Pair<String, () -> Unit>> = when (state.status) {
        UpdateStatus.Available -> listOfNotNull(
            "Download and install" to onDownload,
            ("Release notes" to onOpenNotes).takeIf { state.release?.pageUrl?.isNotBlank() == true },
        )

        UpdateStatus.PermissionNeeded -> listOf("Allow installs" to onGrantPermission)
        UpdateStatus.ReadyToInstall -> listOf("Install now" to onInstall)
        UpdateStatus.Failed -> listOf("Try again" to onCheck)
        UpdateStatus.UpToDate -> listOf("Check again" to onCheck)
        UpdateStatus.Idle, UpdateStatus.Checking, UpdateStatus.Downloading -> emptyList()
    }
    if (actions.isEmpty()) return

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEachIndexed { index, (label, action) ->
            UpdateButton(label = label, filled = index == 0, onClick = action)
        }
    }
}

@Composable
private fun UpdateButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    val shape = FlyDrop.shapes.chip
    Text(
        text = label,
        style = FlyDrop.type.chipLabel,
        color = if (filled) Color.White else colors.violet,
        modifier = modifier
            .clip(shape)
            .background(if (filled) colors.violet else colors.violetSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** A bar rather than a spinner: a multi-megabyte download deserves a fraction. */
@Composable
private fun DownloadProgress(progress: Float?, modifier: Modifier = Modifier) {
    val colors = FlyDrop.colors
    val shape = FlyDrop.shapes.chip
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(colors.paleTile, shape),
    ) {
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(colors.violet, shape),
            )
        }
    }
}

/** What the card says under its title at each step. */
private fun UpdateUiState.subtitle(): String = when (status) {
    UpdateStatus.Idle -> "See if a newer build is on GitHub"
    UpdateStatus.Checking -> "Checking GitHub…"
    UpdateStatus.Downloading -> progress
        ?.let { "Downloading… ${(it * 100).roundToInt()}%" }
        ?: "Downloading…"

    UpdateStatus.Available -> release?.let { "${it.name} is available" } ?: "An update is available"
    UpdateStatus.ReadyToInstall -> message ?: "Downloaded. Follow the installer to finish."
    UpdateStatus.UpToDate, UpdateStatus.PermissionNeeded, UpdateStatus.Failed ->
        message ?: "Something went wrong."
}

/**
 * A row that leaves the app: icon, what it is, and what tapping it does.
 *
 * The trailing mark is the external-link glyph rather than the chevron the
 * in-app rows use, because this one hands the user to a browser and that is
 * worth signalling before the tap rather than after it.
 */
@Composable
private fun LinkCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(FlyDrop.shapes.tile)
                    .background(colors.violetSoft, FlyDrop.shapes.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.violet,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FlyDrop.type.cardTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = FlyDrop.type.metadata,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = FlyDropIcons.ExternalLink,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CreditsTab(info: AboutInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlyDropLogo()
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Version ${info.versionName}",
            style = FlyDrop.type.secondary,
            color = FlyDrop.colors.textSecondary,
        )

        Spacer(Modifier.height(24.dp))
        CreditCard(
            title = "Designed & developed by Vaibhav Sen",
            body = "Made with ❤️ in India 🇮🇳",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        CreditCard(
            title = "Open Source",
            body = "FlyDrop uses open-source software and libraries. " +
                "The app source is released under the MIT License.",
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = "© 2026 Vaibhav Sen. FlyDrop is open source.",
            style = FlyDrop.type.metadata,
            color = FlyDrop.colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A label over its value, matching the cards on Profile. */
@Composable
private fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun CreditCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Text(
                text = title,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = FlyDrop.type.metadata,
                color = FlyDrop.colors.textSecondary,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Enough of a changelog to decide by, without the card becoming the release page. */
private const val NOTE_LINES = 6

/** Matches the sign-in screen's error tone. */
private val ErrorRed = Color(0xFFD1453B)

private val PreviewInfo = AboutInfo(
    versionName = "1.0.1",
    versionCode = 2,
    packageName = "com.flydrop.app",
    debugBuild = false,
    sourceUrl = "https://github.com/vabxsen/flydrop",
    androidRelease = "16",
    sdkInt = 36,
    device = "Google Pixel 9",
)

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun AboutScreenVersionPreview() {
    FlyDropTheme {
        AboutScreen(info = PreviewInfo, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun AboutScreenUpdateAvailablePreview() {
    FlyDropTheme {
        AboutScreen(
            info = PreviewInfo,
            onBack = {},
            updateState = UpdateUiState(
                status = UpdateStatus.Available,
                release = AppRelease(
                    tag = "v1.1.0",
                    name = "FlyDrop v1.1.0",
                    notes = "- Editable FlyDrop IDs\n- Custom profile photos",
                    pageUrl = "https://example.com/releases/v1.1.0",
                    apkUrl = "https://example.com/FlyDrop-v1.1.0.apk",
                    apkBytes = 2_965_982,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun AboutScreenDownloadingPreview() {
    FlyDropTheme {
        AboutScreen(
            info = PreviewInfo,
            onBack = {},
            updateState = UpdateUiState(status = UpdateStatus.Downloading, progress = 0.42f),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun AboutScreenUpToDatePreview() {
    FlyDropTheme {
        AboutScreen(
            info = PreviewInfo,
            onBack = {},
            updateState = UpdateUiState(
                status = UpdateStatus.UpToDate,
                message = "You are on the latest version (1.0.1).",
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun AboutScreenCreditsPreview() {
    FlyDropTheme {
        // The tab is internal state, so the preview shows the pane directly.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlyDrop.colors.surface)
                .padding(FlyDrop.dimens.screenPadding),
        ) {
            CreditsTab(info = PreviewInfo)
        }
    }
}
