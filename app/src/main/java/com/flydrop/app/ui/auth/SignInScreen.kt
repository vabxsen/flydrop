package com.flydrop.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.auth.AuthUnavailableReason
import com.flydrop.app.ui.components.FlyDropLogo
import com.flydrop.app.ui.components.GoogleMark
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * Sign-in.
 *
 * The reference mockup has no auth screen, so this is built from the same
 * vocabulary as the rest of the app: the pale aqua hero, the concentric rings
 * borrowed from the Nearby radar, and a white sheet carrying the action.
 */
@Composable
fun SignInScreen(
    state: AuthUiState,
    onSignIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = FlyDrop.colors
    val dimens = FlyDrop.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.heroAqua),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = contentPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SignInHalo()
            Spacer(Modifier.height(30.dp))
            FlyDropLogo()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Share files with people nearby,\ninstantly and without limits.",
                style = FlyDrop.type.label,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FlyDrop.shapes.sheet)
                .background(colors.surface)
                .padding(horizontal = dimens.screenPadding)
                .padding(top = 26.dp, bottom = contentPadding.calculateBottomPadding() + 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Sign in to continue",
                style = FlyDrop.type.sectionTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your FlyDrop ID travels with your account.",
                style = FlyDrop.type.secondary,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            GoogleSignInButton(
                enabled = state.unavailableReason == null && !state.signingIn,
                loading = state.signingIn,
                onClick = onSignIn,
            )

            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = FlyDrop.type.secondary,
                        color = Color(0xFFD1453B),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (state.unavailableReason != null) {
                Spacer(Modifier.height(16.dp))
                SetupNotice(
                    reason = state.unavailableReason,
                    onContinueAsGuest = onContinueAsGuest,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                GuestModeButton(onClick = onContinueAsGuest)
            }
        }
    }
}

/** The violet ring and soft bloom, echoing the centre of the Nearby radar. */
@Composable
private fun SignInHalo(modifier: Modifier = Modifier) {
    val colors = FlyDrop.colors
    val transition = rememberInfiniteTransition(label = "signInHalo")
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signInHaloBreathe",
    )

    Canvas(modifier = modifier.size(168.dp)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        listOf(1f, 0.70f, 0.42f).forEach { fraction ->
            drawCircle(
                color = colors.radarRing,
                radius = radius * fraction,
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White,
                    0.62f to Color.White,
                    1f to Color.White.copy(alpha = 0f),
                ),
                center = centre,
                radius = radius * 0.52f * breathe,
            ),
            radius = radius * 0.52f * breathe,
            center = centre,
        )
        val ringStroke = 5.dp.toPx()
        drawCircle(
            color = colors.violet,
            radius = 15.dp.toPx() - ringStroke / 2f,
            center = centre,
            style = Stroke(width = ringStroke),
        )
    }
}

/**
 * White button with the four-colour mark, per Google's branding rules. It is
 * deliberately not the app's violet [com.flydrop.app.ui.components.PrimaryActionButton]:
 * recolouring a Google sign-in button is not permitted.
 */
@Composable
private fun GoogleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.button,
        color = if (enabled) FlyDrop.colors.surface else FlyDrop.colors.paleTile,
        elevation = if (enabled) 4.dp else 0.dp,
        onClick = onClick.takeIf { enabled },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlyDrop.dimens.actionButtonHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = FlyDrop.colors.violet,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Signing in…",
                    style = FlyDrop.type.buttonLabel,
                    color = FlyDrop.colors.textSecondary,
                )
            } else {
                GoogleMark()
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    style = FlyDrop.type.buttonLabel,
                    color = if (enabled) {
                        FlyDrop.colors.textPrimary
                    } else {
                        FlyDrop.colors.textTertiary
                    },
                )
            }
        }
    }
}

/** Shown only while Firebase is unconfigured, so the blocker is self-explaining. */
@Composable
private fun SetupNotice(
    reason: AuthUnavailableReason,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (reason) {
        AuthUnavailableReason.FirebaseNotConfigured ->
            "Firebase is not set up yet. Add app/google-services.json from your " +
                "Firebase project, then rebuild."

        AuthUnavailableReason.MissingWebClientId ->
            "Google sign-in is not enabled on this Firebase project. Turn it on " +
                "under Authentication, then re-download google-services.json."
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FlyDrop.shapes.smallCard)
                .background(FlyDrop.colors.paleTile)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = message,
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        GuestModeButton(onClick = onContinueAsGuest)
    }
}

@Composable
private fun GuestModeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = "Continue as guest",
        style = FlyDrop.type.buttonLabel,
        color = FlyDrop.colors.violet,
        modifier = modifier
            .clip(FlyDrop.shapes.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun SignInScreenPreview() {
    FlyDropTheme {
        SignInScreen(
            state = AuthUiState(status = AuthStatus.SignedOut),
            onSignIn = {},
            onContinueAsGuest = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun SignInScreenNotConfiguredPreview() {
    FlyDropTheme {
        SignInScreen(
            state = AuthUiState(
                status = AuthStatus.SignedOut,
                unavailableReason = AuthUnavailableReason.FirebaseNotConfigured,
            ),
            onSignIn = {},
            onContinueAsGuest = {},
        )
    }
}
