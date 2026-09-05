package com.flydrop.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every spacing/size decision in one place, so the whole layout can be nudged
 * toward the reference without hunting through composables.
 *
 * These are measured values, not estimates. The reference mockup renders a
 * phone 355 px wide, which maps 1:1 onto a ~360dp Android screen, so pixel
 * distances read off the image are used here directly as dp.
 */
@Immutable
data class FlyDropDimens(
    val screenPadding: Dp = 20.dp,
    val sectionGap: Dp = 24.dp,
    val cardGap: Dp = 10.dp,

    // Home — profile card is 132 tall: 12 pad + 44 avatar + 12 gap + 51 button + 12 pad
    val topBarHeight: Dp = 56.dp,
    val cardPadding: Dp = 12.dp,
    val profileAvatar: Dp = 44.dp,
    val scanTile: Dp = 38.dp,
    val actionButtonHeight: Dp = 51.dp,
    val actionButtonGap: Dp = 9.dp,

    // Home — Flydrop Web strip is 65 tall: 15 pad + 34 tile + 15 pad
    val webCardHeight: Dp = 65.dp,
    val webCardIcon: Dp = 34.dp,

    val friendCardWidth: Dp = 91.dp,
    val friendCardHeight: Dp = 108.dp,
    val friendAvatar: Dp = 56.dp,

    val activityAvatar: Dp = 40.dp,
    val activityRowHeight: Dp = 64.dp,

    // Floating navigation: 56 tall, inset 36 from each edge, 3 of inner padding
    val navHeight: Dp = 56.dp,
    val navInnerPadding: Dp = 3.dp,
    val navHorizontalMargin: Dp = 36.dp,
    val navBottomInset: Dp = 8.dp,
    val navIcon: Dp = 21.dp,

    // Nearby
    val toggleWidth: Dp = 44.dp,
    val toggleHeight: Dp = 26.dp,
    val nearbyCardWidth: Dp = 131.dp,
    val nearbyCardGap: Dp = 14.dp,
    val nearbyCardAvatar: Dp = 60.dp,
    val nearbyActionHeight: Dp = 29.dp,
    val panelTopPadding: Dp = 20.dp,

    // Transfer — the circle is 206 across on a 356-wide screen (see TransferScreen)
    val statsCardHeight: Dp = 59.dp,
    val fileRowHeight: Dp = 59.dp,
    val fileThumb: Dp = 41.dp,
    val fileProgress: Dp = 28.dp,
)

val LocalFlyDropDimens = staticCompositionLocalOf { FlyDropDimens() }
