package com.flydrop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * Section heading, with the optional violet "See All" action on the right.
 *
 * [trailing] takes the same right-hand slot for headings that need a control
 * rather than a word, such as the contacts search toggle.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
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
        trailing?.invoke()
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

/** A phone contact with an explicit, accessible favourite toggle. */
@Composable
fun ContactItem(
    contact: FlyUser,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
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
                .height(FlyDrop.dimens.activityRowHeight)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(seed = contact.avatarSeed, size = FlyDrop.dimens.activityAvatar)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Phone contact",
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                )
            }
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.semantics {
                    contentDescription = if (isFavourite) {
                        "Remove ${contact.name} from favourites"
                    } else {
                        "Add ${contact.name} to favourites"
                    }
                },
            ) {
                FavouriteStar(isFavourite = isFavourite)
            }
        }
    }
}

/**
 * The favourite star: a violet fill that springs in over the outline.
 *
 * The spring is deliberately underdamped, so the fill overshoots its size and
 * settles back - a small celebration rather than a state flag flipping. Scale
 * and alpha are clamped because an underdamped spring travels outside 0..1 at
 * both ends, and a negative scale would mirror the glyph.
 */
@Composable
private fun FavouriteStar(
    isFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    val fill by animateFloatAsState(
        targetValue = if (isFavourite) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "favouriteStarFill",
    )
    val outlineTint by animateColorAsState(
        targetValue = if (isFavourite) {
            FlyDrop.colors.violet
        } else {
            FlyDrop.colors.textTertiary
        },
        animationSpec = tween(220),
        label = "favouriteStarOutline",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = FlyDropIcons.Star,
            contentDescription = null,
            tint = outlineTint,
            modifier = Modifier.size(22.dp),
        )
        Icon(
            imageVector = FlyDropIcons.StarFilled,
            contentDescription = null,
            tint = FlyDrop.colors.violet,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    val scale = fill.coerceAtLeast(0f)
                    scaleX = scale
                    scaleY = scale
                    alpha = fill.coerceIn(0f, 1f)
                },
        )
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

            SectionHeader(title = "Favourite Friends")
            Text("No favourite friends yet")
            SectionHeader(title = "Contacts")
            MockData.nearbyFriends.take(3).forEach {
                ContactItem(contact = it, isFavourite = false, onToggleFavourite = {})
            }
        }
    }
}
