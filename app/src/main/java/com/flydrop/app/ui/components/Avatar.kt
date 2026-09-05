package com.flydrop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * A generated, deterministic avatar: a soft two-stop gradient disc with a
 * stylised head-and-shoulders figure clipped inside it.
 *
 * The reference mockup uses stock portrait photography, which cannot be shipped.
 * This keeps the same visual weight and warmth at every size the design needs,
 * with no image assets and nothing to load. Swap the body of this composable for
 * an image loader later and every screen picks it up.
 */
@Composable
fun Avatar(
    seed: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    ringColor: Color? = null,
    ringWidth: Dp = 2.5.dp,
) {
    val palette = avatarPalette(seed)
    Canvas(modifier = modifier.size(size)) {
        val d = this.size.minDimension
        val ringPx = if (ringColor != null) ringWidth.toPx() else 0f
        // Leave room for the ring so the face never touches the stroke.
        val gap = if (ringColor != null) ringPx * 0.9f else 0f
        val discRadius = d / 2f - ringPx - gap
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        if (ringColor != null) {
            drawCircle(
                color = ringColor,
                radius = d / 2f - ringPx / 2f,
                center = center,
                style = Stroke(width = ringPx),
            )
        }

        val discRect = Rect(center = center, radius = discRadius)
        val disc = Path().apply { addOval(discRect) }

        clipPath(disc) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(palette.top, palette.bottom),
                    start = Offset(discRect.left, discRect.top),
                    end = Offset(discRect.right, discRect.bottom),
                ),
                topLeft = discRect.topLeft,
                size = discRect.size,
            )
            drawPortrait(discRect, palette, seed)
        }
    }
}

/**
 * A stylised portrait rather than a silhouette.
 *
 * The order matters: hair mass, then torso, then the face disc laid over the
 * hair so a rim of it survives as a hairline. That rim is what stops the shape
 * reading as the stock Android contact glyph.
 */
private fun DrawScope.drawPortrait(bounds: Rect, palette: AvatarPalette, seed: Int) {
    val w = bounds.width
    val h = bounds.height
    // Small deterministic sway so a row of avatars does not look stamped.
    val sway = ((seed * 37) % 7 - 3) / 100f * w

    val headX = bounds.left + w * 0.5f + sway
    val headY = bounds.top + h * 0.415f
    val headRadius = w * 0.215f

    // Hair, sitting just proud of the face so only a crown and temples show.
    // Keeping this tight is what stops it reading as a dark helmet.
    drawOval(
        color = palette.hair,
        topLeft = Offset(headX - headRadius * 1.13f, headY - headRadius * 1.06f),
        size = Size(headRadius * 2.26f, headRadius * 2.00f),
    )

    // Neck. Wide and short — a narrow one reads as a peg at these sizes.
    drawOval(
        color = palette.skin,
        topLeft = Offset(headX - headRadius * 0.52f, headY + headRadius * 0.45f),
        size = Size(headRadius * 1.04f, headRadius * 1.30f),
    )

    // Shoulders, coming up close under the chin and clipped by the disc.
    val shoulderWidth = w * 0.98f
    drawOval(
        color = palette.clothing,
        topLeft = Offset(
            bounds.left + (w - shoulderWidth) / 2f + sway * 0.5f,
            bounds.top + h * 0.700f,
        ),
        size = Size(shoulderWidth, h * 0.64f),
    )

    // Face, leaving the hair visible as a rim around the crown and temples.
    drawCircle(
        color = palette.skin,
        radius = headRadius,
        center = Offset(headX, headY + headRadius * 0.14f),
    )
}

private data class AvatarPalette(
    val top: Color,
    val bottom: Color,
    val hair: Color,
    val skin: Color,
    val clothing: Color,
)

/**
 * Soft, on-brand backgrounds with varied hair, skin and clothing tones, so a
 * row of avatars reads as a group of people rather than a colour chart.
 */
private val avatarPalettes = listOf(
    AvatarPalette(Color(0xFFC3CDFF), Color(0xFF8492F2), Color(0xFF574B6B), Color(0xFFEFC5A3), Color(0xFFEDF1FF)),
    AvatarPalette(Color(0xFFFFD0D5), Color(0xFFF0919F), Color(0xFF7E4B33), Color(0xFFF7D4B8), Color(0xFFFFF1EC)),
    AvatarPalette(Color(0xFFC6F0F1), Color(0xFF74D4D7), Color(0xFF44405A), Color(0xFFDFAE88), Color(0xFFEDFBFB)),
    AvatarPalette(Color(0xFFDBCDFF), Color(0xFFAB8FF5), Color(0xFF6B5240), Color(0xFFF4D0B1), Color(0xFFF4EFFF)),
    AvatarPalette(Color(0xFFFFE5C4), Color(0xFFF3BC7B), Color(0xFF473830), Color(0xFFCF9871), Color(0xFFFFF6E9)),
    AvatarPalette(Color(0xFFCCE7FF), Color(0xFF87BAF1), Color(0xFF5C463C), Color(0xFFEDC29E), Color(0xFFEDF6FF)),
    AvatarPalette(Color(0xFFD7F1D4), Color(0xFF95D091), Color(0xFF4A4038), Color(0xFFE2B391), Color(0xFFF0FBEF)),
)

private fun avatarPalette(seed: Int): AvatarPalette =
    avatarPalettes[Math.floorMod(seed, avatarPalettes.size)]

/** Avatar with a small circular status badge tucked into its lower-right edge. */
@Composable
fun BadgedAvatar(
    seed: Int,
    size: Dp,
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.BottomStart) {
        Avatar(seed = seed, size = size)
        badge()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 420)
@Composable
private fun AvatarPreview() {
    FlyDropTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..6).forEach { seed -> Avatar(seed = seed, size = 52.dp) }
            Avatar(seed = 2, size = 62.dp, ringColor = FlyDrop.colors.violet)
        }
    }
}
