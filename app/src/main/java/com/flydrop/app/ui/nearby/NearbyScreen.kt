package com.flydrop.app.ui.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.PickedFile
import com.flydrop.app.data.formatFileSize
import com.flydrop.app.ui.components.Chevron
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.PrimaryActionButton
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlinx.coroutines.delay

/**
 * Nearby sharing backed by Android's real Sharesheet and Quick Share.
 *
 * FlyDrop does not run a peer-discovery transport of its own, so this screen
 * deliberately avoids a scanning radar, invented peers, or a discoverability
 * switch. Selecting files and choosing a receiver happens in Android's system
 * UI, where the installed Quick Share implementation owns the actual transfer.
 */
@Composable
fun NearbyScreen(
    onPickFiles: () -> Unit,
    modifier: Modifier = Modifier,
    pickedFiles: List<PickedFile> = emptyList(),
    onClearPickedFiles: () -> Unit = {},
    onSendWithQuickShare: () -> Unit = {},
    onReceiveWithQuickShare: () -> Unit = {},
    shareError: String? = null,
    onDismissShareError: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(shareError) {
        if (shareError != null) {
            delay(SHARE_MESSAGE_DURATION_MS)
            onDismissShareError()
        }
    }

    val dimens = FlyDrop.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FlyDrop.colors.nearbyBackground)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.topBarHeight)
                .padding(horizontal = dimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlyDropLogo(suffix = "nearby")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(FlyDrop.shapes.sheet)
                .background(FlyDrop.colors.surface)
                .padding(horizontal = dimens.screenPadding)
                .padding(
                    top = dimens.panelTopPadding,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(title = "Nearby sharing")

            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                shape = FlyDrop.shapes.smallCard,
                elevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(FlyDrop.shapes.tile)
                            .background(FlyDrop.colors.violetSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FlyDropIcons.Radar,
                            contentDescription = null,
                            tint = FlyDrop.colors.violet,
                            modifier = Modifier.size(29.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Share through Android",
                        style = FlyDrop.type.sectionTitle,
                        color = FlyDrop.colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Choose files, then select Quick Share or another nearby app " +
                            "from Android's secure Sharesheet.",
                        style = FlyDrop.type.secondary,
                        color = FlyDrop.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            AnimatedVisibility(
                visible = shareError != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Text(
                    text = shareError.orEmpty(),
                    style = FlyDrop.type.metadata,
                    color = ShareErrorRed,
                )
            }

            if (pickedFiles.isEmpty()) {
                PrimaryActionButton(
                    label = "Choose files to send",
                    icon = FlyDropIcons.Send,
                    containerColor = FlyDrop.colors.violet,
                    onClick = onPickFiles,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PickedFilesBanner(
                    files = pickedFiles,
                    onClear = onClearPickedFiles,
                    onSendWithQuickShare = onSendWithQuickShare,
                )
            }

            QuickShareReceiveRow(onClick = onReceiveWithQuickShare)
        }
    }
}

@Composable
private fun PickedFilesBanner(
    files: List<PickedFile>,
    onClear: () -> Unit,
    onSendWithQuickShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = files.sumOf { it.sizeBytes ?: 0L }.takeIf { it > 0L }
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(FlyDrop.shapes.tile)
                        .background(FlyDrop.colors.violetSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FlyDropIcons.Document,
                        contentDescription = null,
                        tint = FlyDrop.colors.violet,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (files.size == 1) files.first().name else "${files.size} files ready",
                        style = FlyDrop.type.cardTitle,
                        color = FlyDrop.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (total != null) formatFileSize(total) else "Ready to share",
                        style = FlyDrop.type.metadata,
                        color = FlyDrop.colors.textSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(FlyDrop.shapes.chip)
                        .clickable(onClick = onClear)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Clear", style = FlyDrop.type.metadata, color = FlyDrop.colors.violet)
                }
            }
            PrimaryActionButton(
                label = "Open Sharesheet",
                icon = FlyDropIcons.Send,
                containerColor = FlyDrop.colors.violet,
                onClick = onSendWithQuickShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun QuickShareReceiveRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(FlyDrop.shapes.tile)
                    .background(FlyDrop.colors.tealSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FlyDropIcons.FileReceive,
                    contentDescription = null,
                    tint = FlyDrop.colors.tealPressed,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Receive with Quick Share",
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                Text(
                    text = "Open this phone's Quick Share settings",
                    style = FlyDrop.type.metadata,
                    color = FlyDrop.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Chevron()
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun NearbyScreenPreview() {
    FlyDropTheme {
        NearbyScreen(onPickFiles = {})
    }
}

private const val SHARE_MESSAGE_DURATION_MS = 6_000L
private val ShareErrorRed = Color(0xFFD1453B)
