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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * The large white card at the top of Home: identity on the first row, the two
 * transfer actions on the second.
 */
@Composable
fun ProfileCard(
    user: FlyUser,
    onSendFile: () -> Unit,
    onReceiveFile: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = FlyDrop.dimens
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.largeCard,
        elevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(seed = user.avatarSeed, size = dimens.profileAvatar)
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = FlyDrop.type.profileName,
                        color = FlyDrop.colors.textPrimary,
                    )
                    Text(
                        text = user.flyId,
                        style = FlyDrop.type.label,
                        color = FlyDrop.colors.textSecondary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ScanTile(onClick = onScan)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(dimens.actionButtonGap)) {
                PrimaryActionButton(
                    label = "Send File",
                    icon = FlyDropIcons.Send,
                    containerColor = FlyDrop.colors.violet,
                    onClick = onSendFile,
                    modifier = Modifier.weight(1f),
                )
                PrimaryActionButton(
                    label = "Receive File",
                    icon = FlyDropIcons.FileReceive,
                    containerColor = FlyDrop.colors.teal,
                    onClick = onReceiveFile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The pale rounded square holding the scan glyph, at the card's trailing edge. */
@Composable
private fun ScanTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = FlyDrop.shapes.tile
    Box(
        modifier = modifier
            .size(FlyDrop.dimens.scanTile)
            .clip(shape)
            .background(FlyDrop.colors.paleTile, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = FlyDropIcons.ScanFrame,
            contentDescription = "Scan a FlyDrop code",
            tint = FlyDrop.colors.iconMuted,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE3FBFD, widthDp = 360)
@Composable
private fun ProfileCardPreview() {
    FlyDropTheme {
        Box(Modifier.padding(16.dp)) {
            ProfileCard(
                user = MockData.currentUser,
                onSendFile = {},
                onReceiveFile = {},
                onScan = {},
            )
        }
    }
}
