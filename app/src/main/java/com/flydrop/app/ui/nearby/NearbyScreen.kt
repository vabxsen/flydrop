package com.flydrop.app.ui.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.PickedFile
import com.flydrop.app.data.formatFileSize
import com.flydrop.app.data.model.RadarDevice
import com.flydrop.app.data.nearby.NearbyConnectionsState
import com.flydrop.app.data.nearby.NearbyPeer
import com.flydrop.app.ui.components.Chevron
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.PrimaryActionButton
import com.flydrop.app.ui.components.QuickShareSettingsButton
import com.flydrop.app.ui.components.RadarView
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlinx.coroutines.delay

/**
 * Nearby.
 *
 * The radar occupies the upper area directly on the tinted background (it is
 * not inside a card), with the white sharing panel anchored below it.
 *
 * FlyDrop's direct transport is exposed in the sharing panel. The radar remains
 * a non-interactive illustration; its faces are never presented as real peers.
 * Android Quick Share remains available as a separate system-share option.
 */
@Composable
fun NearbyScreen(
    onPickFiles: () -> Unit,
    modifier: Modifier = Modifier,
    devices: List<RadarDevice> = MockData.radarDevices,
    onOpenQuickShareSettings: () -> Unit = {},
    nearbyState: NearbyConnectionsState = NearbyConnectionsState(),
    onStartNearby: () -> Unit = {},
    onStopNearby: () -> Unit = {},
    onSendToNearbyPeer: (NearbyPeer) -> Unit = {},
    onAcceptNearbyConnection: () -> Unit = {},
    onRejectNearbyConnection: () -> Unit = {},
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FlyDrop.colors.nearbyBackground),
    ) {
        // Landscape and split screen leave the radar far too little height to
        // scale into: every device would collapse onto the centre in a heap.
        // The radar is illustration, so below this it simply steps aside and
        // the sharing panel takes the screen.
        val showRadar = maxHeight >= MIN_HEIGHT_FOR_RADAR

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.topBarHeight)
                    .padding(horizontal = dimens.screenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlyDropLogo(suffix = "nearby", modifier = Modifier.weight(1f))
                QuickShareSettingsButton(onClick = onOpenQuickShareSettings)
            }

            // The radar takes the space left between the header and the panel.
            if (showRadar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    RadarView(
                        devices = devices,
                        scanning = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            NearbySharingPanel(
                pickedFiles = pickedFiles,
                onPickFiles = onPickFiles,
                onClearPickedFiles = onClearPickedFiles,
                onSendWithQuickShare = onSendWithQuickShare,
                onReceiveWithQuickShare = onReceiveWithQuickShare,
                nearbyState = nearbyState,
                onStartNearby = onStartNearby,
                onStopNearby = onStopNearby,
                onSendToNearbyPeer = onSendToNearbyPeer,
                shareError = shareError,
                bottomPadding = contentPadding.calculateBottomPadding(),
            )
        }
    }

    nearbyState.incomingConnection?.let { incoming ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onRejectNearbyConnection,
            title = { Text("Nearby FlyDrop device") },
            text = {
                Text(
                    "${incoming.name} wants to connect. Compare this code on both phones before accepting: ${incoming.authenticationToken}",
                )
            },
            confirmButton = { TextButton(onClick = onAcceptNearbyConnection) { Text("Accept") } },
            dismissButton = { TextButton(onClick = onRejectNearbyConnection) { Text("Decline") } },
        )
    }
}

/**
 * The white sheet under the radar: choose files, open the Sharesheet, or go to
 * Quick Share to receive.
 */
@Composable
private fun NearbySharingPanel(
    pickedFiles: List<PickedFile>,
    onPickFiles: () -> Unit,
    onClearPickedFiles: () -> Unit,
    onSendWithQuickShare: () -> Unit,
    onReceiveWithQuickShare: () -> Unit,
    nearbyState: NearbyConnectionsState,
    onStartNearby: () -> Unit,
    onStopNearby: () -> Unit,
    onSendToNearbyPeer: (NearbyPeer) -> Unit,
    shareError: String?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlyDrop.shapes.sheet)
            .background(FlyDrop.colors.surface)
            .padding(horizontal = dimens.screenPadding)
            .padding(top = dimens.panelTopPadding, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            SectionHeader(title = "Nearby sharing")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose files, then pick Quick Share or another nearby app from " +
                    "Android's secure Sharesheet.",
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
            )
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

        DirectFlyDropCard(
            state = nearbyState,
            filesChosen = pickedFiles.isNotEmpty(),
            onStart = onStartNearby,
            onStop = onStopNearby,
            onSend = onSendToNearbyPeer,
        )

        QuickShareReceiveRow(onClick = onReceiveWithQuickShare)
    }
}

/** Real FlyDrop endpoints discovered with Nearby Connections, never radar art. */
@Composable
private fun DirectFlyDropCard(
    state: NearbyConnectionsState,
    filesChosen: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSend: (NearbyPeer) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FlyDropIcons.Radar,
                    contentDescription = null,
                    tint = FlyDrop.colors.tealPressed,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("FlyDrop nearby", style = FlyDrop.type.cardTitle, color = FlyDrop.colors.textPrimary)
                    Text(
                        if (state.active) "Visible to nearby FlyDrop devices" else "Find and receive from FlyDrop devices",
                        style = FlyDrop.type.metadata,
                        color = FlyDrop.colors.textSecondary,
                    )
                }
                Text(
                    text = if (state.active) "Stop" else "Start",
                    style = FlyDrop.type.buttonLabel,
                    color = FlyDrop.colors.violet,
                    modifier = Modifier
                        .clip(FlyDrop.shapes.chip)
                        .clickable(onClick = if (state.active) onStop else onStart)
                        .semantics { role = Role.Button }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            state.message?.let { message ->
                Text(message, style = FlyDrop.type.metadata, color = FlyDrop.colors.textSecondary)
            }
            if (state.active && state.peers.isEmpty()) {
                Text("No FlyDrop devices found yet.", style = FlyDrop.type.metadata, color = FlyDrop.colors.textTertiary)
            }
            state.peers.take(3).forEach { peer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FlyDrop.shapes.tile)
                        .background(FlyDrop.colors.tealSoft)
                        .clickable { onSend(peer) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(peer.name, style = FlyDrop.type.metadata, color = FlyDrop.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (filesChosen) "Send" else "Choose files",
                        style = FlyDrop.type.buttonLabel,
                        color = FlyDrop.colors.violet,
                    )
                }
            }
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

/** Below this the radar has too little height to scale into; see [NearbyScreen]. */
private val MIN_HEIGHT_FOR_RADAR = 600.dp

private const val SHARE_MESSAGE_DURATION_MS = 6_000L
private val ShareErrorRed = Color(0xFFD1453B)
