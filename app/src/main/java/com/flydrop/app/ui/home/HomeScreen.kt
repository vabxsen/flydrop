package com.flydrop.app.ui.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.ActivityEntry
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.ActivityItem
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.FriendCard
import com.flydrop.app.ui.components.ProfileCard
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.WebCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * Home.
 *
 * The defining feature is the two-tone construction: a pale aqua hero holding
 * the top bar, profile card and web card, with a full-bleed white sheet rising
 * over it from "Favourite Friends" down.
 *
 * The list itself is painted white so the sheet continues to the bottom of the
 * screen behind the floating navigation; only the hero item paints aqua.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSendFile: () -> Unit,
    onReceiveFile: () -> Unit,
    onOpenActivity: (ActivityEntry) -> Unit,
    onOpenFriend: (FlyUser) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dimens = FlyDrop.dimens
    val colors = FlyDrop.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.heroAqua)
                    .padding(top = contentPadding.calculateTopPadding())
                    .padding(horizontal = dimens.screenPadding),
            ) {
                HomeTopBar(hasNotifications = state.hasNotifications)
                ProfileCard(
                    user = state.currentUser,
                    onSendFile = onSendFile,
                    onReceiveFile = onReceiveFile,
                    onScan = {},
                )
                Spacer(Modifier.height(14.dp))
                WebCard(onClick = {})
                Spacer(Modifier.height(17.dp))
            }
        }

        // The lip where the white sheet rises over the aqua hero.
        item(key = "sheetTop") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(colors.heroAqua)
                    .clip(FlyDrop.shapes.sheet)
                    .background(colors.surface),
            )
        }

        item(key = "friends") {
            SectionHeader(
                title = "Favourite Friends",
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                items(state.favouriteFriends, key = { it.id }) { friend ->
                    FriendCard(user = friend, onClick = { onOpenFriend(friend) })
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Latest Activities",
                actionLabel = "See All",
                onAction = {},
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(13.dp))
        }

        items(state.activities, key = { it.id }) { entry ->
            ActivityItem(
                entry = entry,
                onClick = { onOpenActivity(entry) },
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(dimens.cardGap))
        }
    }
}

@Composable
private fun HomeTopBar(hasNotifications: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlyDrop.dimens.topBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlyDropLogo(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FlyDropIcons.Bell,
                contentDescription = "Notifications",
                tint = FlyDrop.colors.textPrimary,
                modifier = Modifier.size(21.dp),
            )
            if (hasNotifications) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 5.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(FlyDrop.colors.logoPlane, CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun HomeScreenPreview() {
    FlyDropTheme {
        HomeScreen(
            state = HomeUiState(
                currentUser = MockData.currentUser,
                favouriteFriends = MockData.favouriteFriends,
                activities = MockData.activities,
            ),
            onSendFile = {},
            onReceiveFile = {},
            onOpenActivity = {},
            onOpenFriend = {},
        )
    }
}
