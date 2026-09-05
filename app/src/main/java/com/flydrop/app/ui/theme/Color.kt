package com.flydrop.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Every value below was sampled directly out of the FlyDrop reference mockup
 * (dominant colour over a flat region; darkest pixel for text glyphs) rather
 * than eyeballed, so the palette is the mockup's palette.
 */

// Accents
val Violet = Color(0xFF5C33FD)
val VioletPressed = Color(0xFF4E27E8)
val VioletSoft = Color(0xFFE9E6FD)
val Teal = Color(0xFF17C7CD)
val TealPressed = Color(0xFF12B0B6)
val TealSoft = Color(0xFFDDF8F7)
val LogoPlane = Color(0xFF4278FD)

// Text
val TextPrimary = Color(0xFF0A0B2E)
val TextSecondary = Color(0xFF7A81A0)
val TextTertiary = Color(0xFF9AA3BE)
val IconMuted = Color(0xFF5A6684)

// Backgrounds
val HeroAqua = Color(0xFFE3FBFD)
val NearbyBackground = Color(0xFFF5F8FE)
val TransferBackground = Color(0xFFFEFEFE)
val Surface = Color(0xFFFFFFFF)
val NavContainer = Color(0xFFF1F5FB)
val PaleTile = Color(0xFFF4F7FD)

// Lines
val HairlineBorder = Color(0xFFEEF1F8)
val RadarRing = Color(0xFFE7EBF5)
val Divider = Color(0xFFEDF0F7)

/** Semantic palette, so screens never reach for a raw [Color]. */
@Immutable
data class FlyDropColors(
    val violet: Color = Violet,
    val violetPressed: Color = VioletPressed,
    val violetSoft: Color = VioletSoft,
    val teal: Color = Teal,
    val tealPressed: Color = TealPressed,
    val tealSoft: Color = TealSoft,
    val logoPlane: Color = LogoPlane,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val iconMuted: Color = IconMuted,
    val heroAqua: Color = HeroAqua,
    val nearbyBackground: Color = NearbyBackground,
    val transferBackground: Color = TransferBackground,
    val surface: Color = Surface,
    val navContainer: Color = NavContainer,
    val paleTile: Color = PaleTile,
    val hairline: Color = HairlineBorder,
    val radarRing: Color = RadarRing,
    val divider: Color = Divider,
)

val LocalFlyDropColors = staticCompositionLocalOf { FlyDropColors() }
