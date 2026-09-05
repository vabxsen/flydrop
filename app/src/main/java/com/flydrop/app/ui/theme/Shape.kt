package com.flydrop.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii measured from the reference. Nothing in FlyDrop has a sharp corner. */
@Immutable
data class FlyDropShapes(
    /** The white panel that rises over the tinted hero area. */
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val largeCard: Shape = RoundedCornerShape(22.dp),
    val card: Shape = RoundedCornerShape(20.dp),
    val smallCard: Shape = RoundedCornerShape(16.dp),
    val button: Shape = RoundedCornerShape(16.dp),
    val chip: Shape = RoundedCornerShape(11.dp),
    val tile: Shape = RoundedCornerShape(12.dp),
    val fileTile: Shape = RoundedCornerShape(14.dp),
    val navContainer: Shape = RoundedCornerShape(30.dp),
    val navPill: Shape = RoundedCornerShape(24.dp),
)

val LocalFlyDropShapes = staticCompositionLocalOf { FlyDropShapes() }

val FlyDropMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
