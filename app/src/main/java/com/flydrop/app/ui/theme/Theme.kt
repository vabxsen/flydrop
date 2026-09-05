package com.flydrop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val FlyDropColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Surface,
    primaryContainer = VioletSoft,
    onPrimaryContainer = Violet,
    secondary = Teal,
    onSecondary = Surface,
    secondaryContainer = TealSoft,
    onSecondaryContainer = Teal,
    background = NearbyBackground,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = PaleTile,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = Divider,
)

/**
 * Dynamic colour is deliberately never used: the reference has a fixed brand
 * palette and Material You would pull it off-design.
 */
@Composable
fun FlyDropTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFlyDropColors provides FlyDropColors(),
        LocalFlyDropTypography provides FlyDropTypography(),
        LocalFlyDropShapes provides FlyDropShapes(),
        LocalFlyDropDimens provides FlyDropDimens(),
    ) {
        MaterialTheme(
            colorScheme = FlyDropColorScheme,
            typography = FlyDropMaterialTypography,
            shapes = FlyDropMaterialShapes,
            content = content,
        )
    }
}

/** Short accessors: `FlyDrop.colors`, `FlyDrop.type`, `FlyDrop.shapes`, `FlyDrop.dimens`. */
object FlyDrop {
    val colors: FlyDropColors
        @Composable @ReadOnlyComposable get() = LocalFlyDropColors.current
    val type: FlyDropTypography
        @Composable @ReadOnlyComposable get() = LocalFlyDropTypography.current
    val shapes: FlyDropShapes
        @Composable @ReadOnlyComposable get() = LocalFlyDropShapes.current
    val dimens: FlyDropDimens
        @Composable @ReadOnlyComposable get() = LocalFlyDropDimens.current
}
