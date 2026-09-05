package com.flydrop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.flydrop.app.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val TrimBoth = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun poppins(
    size: Int,
    weight: FontWeight,
    lineHeight: Int = (size * 1.35f).toInt(),
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = TrimBoth,
)

/**
 * The type ramp read off the reference, named by role rather than by Material slot.
 *
 * Sizes are derived from measured cap-heights in the mockup (Poppins cap height
 * is 0.70em, descender 0.21em), not guessed. The scale is noticeably tighter
 * than Material defaults, which is a large part of why the reference reads as
 * dense and premium rather than like a stock Android screen.
 */
@Immutable
data class FlyDropTypography(
    val logo: TextStyle = poppins(19, FontWeight.Bold, letterSpacing = (-0.4f)),
    val logoMuted: TextStyle = poppins(19, FontWeight.Medium, letterSpacing = (-0.4f)),
    val screenTitle: TextStyle = poppins(20, FontWeight.Bold, letterSpacing = (-0.2f)),
    val sectionTitle: TextStyle = poppins(17, FontWeight.Bold, letterSpacing = (-0.2f)),
    val profileName: TextStyle = poppins(15, FontWeight.SemiBold, letterSpacing = (-0.1f)),
    val cardTitle: TextStyle = poppins(14, FontWeight.SemiBold),
    val buttonLabel: TextStyle = poppins(13, FontWeight.SemiBold),
    val statValue: TextStyle = poppins(13, FontWeight.SemiBold),
    val friendName: TextStyle = poppins(13, FontWeight.SemiBold),
    val label: TextStyle = poppins(12, FontWeight.Medium),
    val secondary: TextStyle = poppins(12, FontWeight.Normal),
    val metadata: TextStyle = poppins(11, FontWeight.Normal),
    val chipLabel: TextStyle = poppins(12, FontWeight.SemiBold),
)

val LocalFlyDropTypography = staticCompositionLocalOf { FlyDropTypography() }

/** Material 3 fallback ramp so stock components still render in Poppins. */
val FlyDropMaterialTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Poppins),
        displayMedium = displayMedium.copy(fontFamily = Poppins),
        displaySmall = displaySmall.copy(fontFamily = Poppins),
        headlineLarge = headlineLarge.copy(fontFamily = Poppins),
        headlineMedium = headlineMedium.copy(fontFamily = Poppins),
        headlineSmall = headlineSmall.copy(fontFamily = Poppins),
        titleLarge = titleLarge.copy(fontFamily = Poppins),
        titleMedium = titleMedium.copy(fontFamily = Poppins),
        titleSmall = titleSmall.copy(fontFamily = Poppins),
        bodyLarge = bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = bodyMedium.copy(fontFamily = Poppins),
        bodySmall = bodySmall.copy(fontFamily = Poppins),
        labelLarge = labelLarge.copy(fontFamily = Poppins),
        labelMedium = labelMedium.copy(fontFamily = Poppins),
        labelSmall = labelSmall.copy(fontFamily = Poppins),
    )
}
