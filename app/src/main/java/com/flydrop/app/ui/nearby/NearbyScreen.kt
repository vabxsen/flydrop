package com.flydrop.app.ui.nearby

import androidx.compose.animation.core.spring
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.flydrop.app.ui.components.RadarView
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

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
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
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

        if (pickedFiles.isNotEmpty()) {
            PickedFilesBanner(
                files = pickedFiles,
                onClear = onClearPickedFiles,
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
            bottomPadding = contentPadding.calculateBottomPadding(),
        )
    }
}

/**
 * What Send File picked, waiting on a recipient.
 *
 * Nothing can actually be sent yet - there is no transport - so this says only
 * what is true: the files are chosen, and a device has to be picked next.
 */
@Composable
private fun PickedFilesBanner(
    files: List<PickedFile>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = files.sumOf { it.sizeBytes ?: 0L }.takeIf { it > 0L }

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
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
                    text = if (total != null) {
                        "${formatFileSize(total)} · choose a device below"
                    } else {
                        "Choose a device below"
                    },
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
    }
}

@Composable
private fun NearbyFriendsPanel(
    friends: List<FlyUser>,
    discovering: Boolean,
    selectedUserId: String?,
    onSelectUser: (String) -> Unit,
    onAddFriend: (FlyUser) -> Unit,
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
