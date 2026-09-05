package com.flydrop.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * The Quick Share visibility switch in the Nearby header. Material's
 * [androidx.compose.material3.Switch] has a visible outline, a smaller thumb
 * and a wider track than the reference, so this is drawn directly.
 */
@Composable
fun DiscoverySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    val haptics = LocalHapticFeedback.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) FlyDrop.colors.violet else Color(0xFFD5DCEC),
        animationSpec = tween(durationMillis = 240),
        label = "switchTrack",
    )
    val thumbPadding = 3.dp
    val thumbSize = dimens.toggleHeight - thumbPadding * 2
    // Kept as State (not unwrapped) so the offset can be read at draw time
    // rather than recomposing the whole switch on every animation frame.
    val thumbOffset = animateDpAsState(
        targetValue = if (checked) dimens.toggleWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = tween(durationMillis = 240),
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .size(width = dimens.toggleWidth, height = dimens.toggleHeight)
            .clip(CircleShape)
            .background(trackColor, CircleShape)
            .semantics { contentDescription = "Quick Share visibility" }
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = dimens.toggleWidth / 2),
                role = Role.Switch,
                onValueChange = { newValue ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(newValue)
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.value.roundToPx(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * A card in the "Nearby Friends" strip: avatar, name, FlyDrop id, and either a
 * muted "Friends" chip or a solid "Add Friends" button.
 */
@Composable
fun NearbyFriendCard(
    user: FlyUser,
    selected: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    SoftCard(
        modifier = modifier.width(dimens.nearbyCardWidth),
        shape = FlyDrop.shapes.card,
        borderColor = if (selected) FlyDrop.colors.violet else FlyDrop.colors.hairline,
        elevation = if (selected) 6.dp else 3.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(seed = user.avatarSeed, size = dimens.nearbyCardAvatar)
            Spacer(Modifier.height(13.dp))
            Text(
                text = user.name,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = user.flyId,
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(11.dp))
            if (user.isFriend) {
                FriendStateChip(modifier = Modifier.fillMaxWidth())
            } else {
                AddFriendButton(
                    userName = user.name,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FriendStateChip(modifier: Modifier = Modifier) {
    val shape = FlyDrop.shapes.chip
    Box(
        modifier = modifier
            .height(FlyDrop.dimens.nearbyActionHeight)
            .clip(shape)
            .background(FlyDrop.colors.violetSoft, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Friends",
            style = FlyDrop.type.chipLabel,
            color = FlyDrop.colors.violet,
        )
    }
}

@Composable
private fun AddFriendButton(
    userName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FlyDrop.shapes.chip
    Row(
        modifier = modifier
            .height(FlyDrop.dimens.nearbyActionHeight)
            .clip(shape)
            .background(FlyDrop.colors.violet, shape)
            .semantics { contentDescription = "Add $userName as friend" }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FlyDropIcons.Plus,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Add Friends",
            style = FlyDrop.type.chipLabel,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F8FE, widthDp = 380)
@Composable
private fun NearbyComponentsPreview() {
    FlyDropTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiscoverySwitch(checked = true, onCheckedChange = {})
                DiscoverySwitch(checked = false, onCheckedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NearbyFriendCard(
                    user = MockData.ashley,
                    selected = true,
                    onClick = {},
                    onAction = {},
                )
                NearbyFriendCard(
                    user = MockData.gofar,
                    selected = false,
                    onClick = {},
                    onAction = {},
                )
            }
        }
    }
}
