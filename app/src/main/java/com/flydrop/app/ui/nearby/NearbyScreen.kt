package com.flydrop.app.ui.nearby

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.DiscoverySwitch
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.NearbyFriendCard
import com.flydrop.app.ui.components.RadarView
import com.flydrop.app.ui.components.SectionHeader
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
            selectedUserId = state.selectedUserId,
            onSelectUser = onSelectUser,
            onAddFriend = onAddFriend,
            bottomPadding = contentPadding.calculateBottomPadding(),
        )
    }
}

@Composable
private fun NearbyFriendsPanel(
    friends: List<FlyUser>,
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
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun NearbyScreenPreview() {
    FlyDropTheme {
        NearbyScreen(
            state = NearbyUiState(devices = MockData.radarDevices),
            onDiscoverableChange = {},
            onSelectUser = {},
            onAddFriend = {},
        )
    }
}
