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
import java.net.URLEncoder
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

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
                AboutTab.Version -> VersionTab(info, onOpenLink = openLink)
                AboutTab.Credits -> CreditsTab()
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
    onOpenLink: (String) -> Unit,
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
private fun CreditsTab(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        CreditCard(
            title = "FlyDrop",
            body = "Copyright © 2026 Vaibhav Sen. Released under the MIT " +
                "License; see LICENSE in the project root.",
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        CreditCard(
            title = "Poppins",
            body = "The typeface throughout, used under the SIL Open Font " +
                "License 1.1. The full licence text is bundled at " +
                "licenses/Poppins-OFL.txt.",
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        CreditCard(
            title = "Jetpack Compose & Material 3",
            body = "Kotlin, Jetpack Compose, Material 3 and Navigation Compose, " +
                "under the Apache License 2.0.",
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        CreditCard(
            title = "Firebase & Credential Manager",
            body = "Firebase Authentication and Cloud Firestore hold the account " +
                "and its FlyDrop ID. Sign-in runs through AndroidX Credential " +
                "Manager with Google Identity.",
        )
        Spacer(Modifier.height(FlyDrop.dimens.cardGap))
        CreditCard(
            title = "Artwork",
            body = "There are no image assets. Avatars, icons, the discovery " +
                "radar and the transfer arc are all drawn in Compose from code, " +
                "so nothing is fetched and every screen renders offline.",
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Built with Kotlin and Jetpack Compose.",
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
private fun CreditCard(title: String, body: String, modifier: Modifier = Modifier) {
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
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = FlyDrop.type.metadata,
                color = FlyDrop.colors.textSecondary,
            )
        }
    }
}

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
private fun AboutScreenCreditsPreview() {
    FlyDropTheme {
        // The tab is internal state, so the preview shows the pane directly.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FlyDrop.colors.surface)
                .padding(FlyDrop.dimens.screenPadding),
        ) {
            CreditsTab()
        }
    }
}
