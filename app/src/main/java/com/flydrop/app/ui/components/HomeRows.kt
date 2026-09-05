package com.flydrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.ActivityEntry
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.TransferDirection
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/** Section heading, with the optional violet "See All" action on the right. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = FlyDrop.type.sectionTitle,
            color = FlyDrop.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.violet,
                modifier = Modifier
                    .clip(FlyDrop.shapes.chip)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** The horizontal "Flydrop Web" promo strip below the profile card. */
@Composable
fun WebCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.card,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlyDrop.dimens.webCardHeight)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(FlyDrop.dimens.webCardIcon)
                    .clip(FlyDrop.shapes.tile)
                    .background(FlyDrop.colors.paleTile, FlyDrop.shapes.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FlyDropIcons.CloudTransfer,
                    contentDescription = null,
                    tint = FlyDrop.colors.violet,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Flydrop Web",
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                Text(
                    text = "Easiest way to transfer from PC, no boundary",
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

/** A single favourite-friend tile in the horizontal strip. */
@Composable
fun FriendCard(
    user: FlyUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    SoftCard(
        modifier = modifier.size(width = dimens.friendCardWidth, height = dimens.friendCardHeight),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 11.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(seed = user.avatarSeed, size = dimens.friendAvatar)
            Spacer(Modifier.height(9.dp))
            Text(
                text = user.name.substringBefore(' '),
                style = FlyDrop.type.friendName,
                color = FlyDrop.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One row in "Latest Activities". */
@Composable
fun ActivityItem(
    entry: ActivityEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val incoming = entry.direction == TransferDirection.Incoming
    val verb = if (incoming) "Receive from" else "Send to"
    val badgeColor = if (incoming) FlyDrop.colors.teal else FlyDrop.colors.violet

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlyDrop.dimens.activityRowHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionAvatar(
                seed = entry.peer.avatarSeed,
                incoming = incoming,
                badgeColor = badgeColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$verb ${entry.peer.name.substringBefore(' ')}",
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${entry.fileCount} Files • ${entry.relativeTime}",
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

/** Avatar with the small direction badge overlapping its lower-left edge. */
@Composable
private fun DirectionAvatar(
    seed: Int,
    incoming: Boolean,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    val size = FlyDrop.dimens.activityAvatar
    Box(modifier = modifier.size(size)) {
        Avatar(seed = seed, size = size)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-2).dp, y = 2.dp)
                .size(17.dp)
                .clip(CircleShape)
                .background(Color.White, CircleShape)
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (incoming) FlyDropIcons.ArrowDown else FlyDropIcons.ArrowUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(9.dp),
            )
        }
    }
}

@Composable
internal fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = FlyDropIcons.ChevronRight,
        contentDescription = null,
        tint = FlyDrop.colors.textTertiary,
        modifier = modifier.size(17.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun HomeRowsPreview() {
    FlyDropTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WebCard(onClick = {})
            SectionHeader(title = "Favourite Friends")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MockData.favouriteFriends.take(3).forEach {
                    FriendCard(user = it, onClick = {})
                }
            }
            SectionHeader(title = "Latest Activities", actionLabel = "See All", onAction = {})
            MockData.activities.forEach {
                ActivityItem(entry = it, onClick = {})
            }
        }
    }
}
