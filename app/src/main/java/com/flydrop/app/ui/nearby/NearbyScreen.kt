package com.flydrop.app.ui.nearby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.PickedFile
import com.flydrop.app.data.formatFileSize
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.DiscoverySwitch
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.NearbyFriendCard
import com.flydrop.app.ui.components.PrimaryActionButton
import com.flydrop.app.ui.components.RadarView
import com.flydrop.app.ui.components.Chevron
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlinx.coroutines.delay

/**
 * Nearby.
 *
 * The radar occupies the upper area directly on the tinted background (it is
 * not inside a card), with the white "Nearby Friends" panel anchored below it.
 */
@Composable
fun NearbyScreen(
    state: NearbyUiState,
    onDiscoverableChange: (Boolean) -> Unit,
    onSelectUser: (String) -> Unit,
    onAddFriend: (FlyUser) -> Unit,
    modifier: Modifier = Modifier,
    /** Files chosen with Send File, waiting for a recipient. */
    pickedFiles: List<PickedFile> = emptyList(),
    onClearPickedFiles: () -> Unit = {},
    onSendWithQuickShare: () -> Unit = {},
    onReceiveWithQuickShare: () -> Unit = {},
    /** Set when a share or receive attempt could not be started. */
    shareError: String? = null,
    onDismissShareError: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // Long enough to read, then gone - the same treatment Profile gives its
    // messages, rather than leaving an error sitting on the screen.
    LaunchedEffect(shareError) {
        if (shareError != null) {
            delay(SHARE_MESSAGE_DURATION_MS)
            onDismissShareError()
        }
    }

    val dimens = FlyDrop.dimens
    val colors = FlyDrop.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.nearbyBackground)
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
            DiscoverySwitch(
                checked = state.discoverable,
                onCheckedChange = onDiscoverableChange,
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
                modifier = Modifier.padding(
                    start = dimens.screenPadding,
                    end = dimens.screenPadding,
                    bottom = 8.dp,
                ),
            )
        }

        if (pickedFiles.isNotEmpty()) {
            PickedFilesBanner(
                files = pickedFiles,
                onClear = onClearPickedFiles,
                onSendWithQuickShare = onSendWithQuickShare,
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
        }

        // The radar takes the space left between the header and the panel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            RadarView(
                devices = state.devices,
                scanning = state.discoverable,
                selectedUserId = state.selectedUserId,
                onDeviceClick = { onSelectUser(it.user.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }

        NearbyFriendsPanel(
            friends = state.nearbyFriends,
            discovering = state.discoverable,
            selectedUserId = state.selectedUserId,
            onSelectUser = onSelectUser,
            onAddFriend = onAddFriend,
            onReceiveWithQuickShare = onReceiveWithQuickShare,
            bottomPadding = contentPadding.calculateBottomPadding(),
        )
    }
}

/**
 * What Send File picked, and the way to send it.
 *
 * Quick Share does the transfer: this app has no transport of its own, and the
 * receiving phone needs no copy of it. The radar below stays what it is - a
 * view of FlyDrop peers - so the two are kept visually separate.
 */
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
                        .background(FlyDrop.colors.violetSoft, FlyDrop.shapes.tile),
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
                        text = if (files.size == 1) {
                            files.first().name
                        } else {
                            "${files.size} files ready to send"
                        },
                        style = FlyDrop.type.cardTitle,
                        color = FlyDrop.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        // The title already says these are ready to send, so
                        // this carries only what it does not: how much.
                        text = if (total != null) formatFileSize(total) else "Ready to send",
                        style = FlyDrop.type.metadata,
                        color = FlyDrop.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(FlyDrop.shapes.chip)
                        .clickable(onClick = onClear)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Clear",
                        style = FlyDrop.type.metadata,
                        color = FlyDrop.colors.violet,
                    )
                }
            }

            PrimaryActionButton(
                label = "Send with Quick Share",
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

/**
 * The receiving half. Quick Share receiving is switched on by the system, not
 * by an app - there is no public API for it - so this opens the nearest system
 * screen and leaves the switch to the user.
 */
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
                    .background(FlyDrop.colors.tealSoft, FlyDrop.shapes.tile),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Open your phone's Quick Share settings",
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

@Composable
private fun NearbyFriendsPanel(
    friends: List<FlyUser>,
    discovering: Boolean,
    selectedUserId: String?,
    onSelectUser: (String) -> Unit,
    onAddFriend: (FlyUser) -> Unit,
    onReceiveWithQuickShare: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlyDrop.shapes.sheet)
            .background(FlyDrop.colors.surface)
            .padding(top = FlyDrop.dimens.panelTopPadding, bottom = bottomPadding),
    ) {
        SectionHeader(
            title = "Nearby Friends",
            modifier = Modifier.padding(horizontal = dimens.screenPadding),
        )
        Spacer(Modifier.height(12.dp))
        if (friends.isEmpty()) {
            Text(
                text = if (discovering) {
                    "Searching for nearby friends…"
                } else {
                    "Nearby discovery is off."
                },
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.nearbyCardGap),
            ) {
                items(friends, key = { it.id }) { friend ->
                    NearbyFriendCard(
                        user = friend,
                        selected = friend.id == selectedUserId,
                        onClick = { onSelectUser(friend.id) },
                        onAction = { onAddFriend(friend) },
                        modifier = Modifier.animateItem(
                            placementSpec = spring(stiffness = 400f),
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        QuickShareReceiveRow(
            onClick = onReceiveWithQuickShare,
            modifier = Modifier.padding(horizontal = dimens.screenPadding),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun NearbyScreenPreview() {
    FlyDropTheme {
        NearbyScreen(
            state = NearbyUiState(
                devices = MockData.radarDevices,
                nearbyFriends = MockData.nearbyFriends,
                selectedUserId = MockData.ashley.id,
            ),
            onDiscoverableChange = {},
            onSelectUser = {},
            onAddFriend = {},
        )
    }
}

/** Long enough to read the message, short enough not to become furniture. */
private const val SHARE_MESSAGE_DURATION_MS = 6_000L

/** Matches the error tone used on Profile and the sign-in screen. */
private val ShareErrorRed = Color(0xFFD1453B)
