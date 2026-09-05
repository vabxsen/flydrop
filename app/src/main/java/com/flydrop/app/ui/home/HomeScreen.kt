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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.ContactItem
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.FriendCard
import com.flydrop.app.ui.components.ProfileCard
import com.flydrop.app.ui.components.SectionHeader

import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * Home.
 *
 * The defining feature is the two-tone construction: a pale aqua hero holding
 * the top bar, profile card and FlyDrop ID search, with a full-bleed white sheet
 * rising over it from "Favourite Friends" down.
 *
 * The list itself is painted white so the sheet continues to the bottom of the
 * screen behind the floating navigation; only the hero item paints aqua.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSendFile: () -> Unit,
    onReceiveFile: () -> Unit,
    onNotificationsClick: () -> Unit,
    onScan: () -> Unit,
    onOpenFriend: (FlyUser) -> Unit,
    onToggleFavourite: (FlyUser) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRetryContacts: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    /** Opens the invite prompt for a phone contact. */
    onContactClick: (FlyUser) -> Unit,
    modifier: Modifier = Modifier,
    /** The signed-in user's own profile photo, when they have set one. */
    avatar: ImageBitmap? = null,
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
                HomeTopBar(
                    hasNotifications = state.hasNotifications,
                    onNotificationsClick = onNotificationsClick,
                )
                ProfileCard(
                    user = state.currentUser,
                    photo = avatar,
                    onSendFile = onSendFile,
                    onReceiveFile = onReceiveFile,
                    onScan = onScan,
                )
                Spacer(Modifier.height(14.dp))
                FlyIdSearchCard(
                    state = state.search,
                    onQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onClear = onClearSearch,
                    onOpenResult = onOpenFriend,
                )
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
            if (state.favouriteFriends.isEmpty()) {
                Text(
                    text = "No favourite friends yet",
                    style = FlyDrop.type.secondary,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = dimens.screenPadding),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = dimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(dimens.cardGap),
                ) {
                    items(state.favouriteFriends, key = { it.id }) { friend ->
                        FriendCard(user = friend, onClick = { onOpenFriend(friend) })
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionHeader(
                title = "Contacts",
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(13.dp))
        }

        when (state.contactsAccess) {
            ContactsAccess.Ready -> {
                if (state.contacts.isEmpty()) {
                    item(key = "noContacts") {
                        ContactsMessage("No contacts found on this phone")
                    }
                } else {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactItem(
                            contact = contact,
                            isFavourite = state.favouriteFriends.any { it.id == contact.id },
                            onToggleFavourite = { onToggleFavourite(contact) },
                            onClick = { onContactClick(contact) },
                            modifier = Modifier.padding(horizontal = dimens.screenPadding),
                        )
                        Spacer(Modifier.height(dimens.cardGap))
                    }
                }
            }

            ContactsAccess.PermissionRequired,
            ContactsAccess.Requesting,
            -> item(key = "requestingContacts") {
                ContactsMessage("Waiting for contacts permission…")
            }

            ContactsAccess.Loading -> item(key = "loadingContacts") {
                ContactsMessage("Loading contacts…")
            }

            ContactsAccess.Denied -> item(key = "contactsDenied") {
                ContactsMessage(
                    message = "Allow contacts access to show names from this phone.",
                    actionLabel = "Allow contacts",
                    onAction = onRequestContactsPermission,
                )
            }

            ContactsAccess.Error -> item(key = "contactsError") {
                ContactsMessage(
                    message = "Contacts couldn't be loaded.",
                    actionLabel = "Try again",
                    onAction = onRetryContacts,
                )
            }
        }
    }
}

@Composable
private fun ContactsMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FlyDrop.dimens.screenPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = FlyDrop.type.secondary,
            color = FlyDrop.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    hasNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .clickable(onClick = onNotificationsClick),
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
                        .background(FlyDrop.colors.logoPlane, CircleShape)
                        .semantics { contentDescription = "Unread notifications" },
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
                contacts = MockData.nearbyFriends,
                contactsAccess = ContactsAccess.Ready,
            ),
            onSendFile = {},
            onReceiveFile = {},
            onNotificationsClick = {},
            onScan = {},
            onOpenFriend = {},
            onToggleFavourite = {},
            onRequestContactsPermission = {},
            onRetryContacts = {},
            onSearchQueryChange = {},
            onSearch = {},
            onClearSearch = {},
            onContactClick = {},
        )
    }
}
