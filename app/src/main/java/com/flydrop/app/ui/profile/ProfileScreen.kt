package com.flydrop.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.Avatar
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * Profile.
 *
 * The reference does not show this screen, so it is assembled from the same
 * parts as the others: the aqua hero, a white sheet, and the app's card
 * treatment. It carries the signed-in account and the way back out of it.
 */
@Composable
fun ProfileScreen(
    user: FlyUser,
    modifier: Modifier = Modifier,
    signedIn: Boolean = false,
    onSignOut: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = FlyDrop.colors
    val dimens = FlyDrop.dimens

    // Aqua behind everything, so the white sheet's rounded top corners have
    // something to reveal — the same lip Home and Nearby use.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.heroAqua),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.heroAqua)
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Avatar(seed = user.avatarSeed, size = 88.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = user.name,
                style = FlyDrop.type.screenTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = user.email ?: user.flyId,
                style = FlyDrop.type.label,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(FlyDrop.shapes.sheet)
                .background(colors.surface)
                .padding(horizontal = dimens.screenPadding)
                .padding(
                    top = dimens.panelTopPadding,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            InfoCard(label = "FlyDrop ID", value = user.flyId)
            Spacer(Modifier.height(dimens.cardGap))
            InfoCard(
                label = "Account",
                value = if (signedIn) "Signed in with Google" else "Not signed in",
            )

            Spacer(Modifier.height(dimens.sectionGap))
            AccountActionButton(
                signedIn = signedIn,
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textTertiary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun AccountActionButton(
    signedIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FlyDrop.shapes.button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlyDrop.dimens.actionButtonHeight)
            .clip(shape)
            .background(FlyDrop.colors.violetSoft, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FlyDropIcons.ArrowLeft,
            contentDescription = null,
            tint = FlyDrop.colors.violet,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (signedIn) "Sign out" else "Sign in",
            style = FlyDrop.type.buttonLabel,
            color = FlyDrop.colors.violet,
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenPreview() {
    FlyDropTheme {
        ProfileScreen(
            user = MockData.currentUser.copy(email = "lucas@example.com"),
            signedIn = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenSignedOutPreview() {
    FlyDropTheme {
        ProfileScreen(user = MockData.currentUser)
    }
}
