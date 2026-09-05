package com.flydrop.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.RadarDevice
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A decorative signal dot scattered on the rings. */
private data class RadarDot(
    val angleDegrees: Float,
    val radiusFraction: Float,
    val size: Dp,
    val teal: Boolean,
)

private val radarDots = listOf(
    RadarDot(angleDegrees = -63f, radiusFraction = 0.97f, size = 15.dp, teal = false),
    RadarDot(angleDegrees = 33f, radiusFraction = 0.52f, size = 10.dp, teal = true),
    RadarDot(angleDegrees = 118f, radiusFraction = 0.93f, size = 11.dp, teal = false),
    RadarDot(angleDegrees = 214f, radiusFraction = 0.60f, size = 10.dp, teal = true),
)

/** Ring radii as a fraction of the radar's outer radius. */
private val ringFractions = listOf(1f, 0.70f, 0.42f)

/**
 * The circular discovery visualisation: thin concentric rings, a soft glowing
 * centre that pulses while scanning, scattered signal dots, and the discovered
 * devices placed around the rings.
 *
 * Positions are polar (angle + radius fraction) and resolved against the
 * measured size, so the whole composition scales with the screen instead of
 * being pinned to fixed coordinates.
 */
@Composable
fun RadarView(
    devices: List<RadarDevice>,
    scanning: Boolean,
    modifier: Modifier = Modifier,
    selectedUserId: String? = null,
    onDeviceClick: (RadarDevice) -> Unit = {},
) {
    val colors = FlyDrop.colors
    val transition = rememberInfiniteTransition(label = "radar")

    // One slow sweep outward; calm rather than attention-grabbing.
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarPulse",
    )
    // Gentle breathing of the central glow.
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radarBreathe",
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val outerRadius: Dp = min(maxWidth.value, maxHeight.value).dp / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radiusPx = outerRadius.toPx()

            ringFractions.forEach { fraction ->
                drawCircle(
                    color = colors.radarRing,
                    radius = radiusPx * fraction,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            if (scanning) {
                drawPulse(center, radiusPx, pulse, colors.violet)
            }

            drawCentreGlow(center, radiusPx * 0.36f * breathe, colors.violet)
        }

        radarDots.forEach { dot ->
            Box(
                modifier = Modifier
                    .polarOffset(dot.angleDegrees, outerRadius * dot.radiusFraction)
                    .size(dot.size),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = if (dot.teal) colors.teal else colors.violet)
                }
            }
        }

        // The violet ring marking "you" at the centre.
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = 5.dp.toPx()
            drawCircle(
                color = colors.violet,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke),
            )
        }

        devices.forEach { device ->
            Avatar(
                seed = device.user.avatarSeed,
                size = device.avatarSize.dp,
                ringColor = if (device.user.id == selectedUserId) colors.violet else null,
                modifier = Modifier
                    .polarOffset(device.angleDegrees, outerRadius * device.radiusFraction)
                    .clip(CircleShape)
                    .clickable { onDeviceClick(device) },
            )
        }
    }
}

/**
 * Offsets a centred child onto polar coordinates, with 0 degrees at 12 o'clock
 * and positive angles running clockwise.
 */
private fun Modifier.polarOffset(angleDegrees: Float, radius: Dp): Modifier {
    val radians = Math.toRadians((angleDegrees - 90f).toDouble())
    return this.offset(
        x = radius * cos(radians).toFloat(),
        y = radius * sin(radians).toFloat(),
    )
}

/** An expanding, fading ring that reads as an outgoing discovery ping. */
private fun DrawScope.drawPulse(center: Offset, radiusPx: Float, progress: Float, color: Color) {
    val pulseRadius = radiusPx * (0.30f + progress * 0.70f)
    val alpha = (1f - progress) * 0.16f
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = pulseRadius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

/** Soft white bloom with a faint violet core, matching the reference centre. */
private fun DrawScope.drawCentreGlow(center: Offset, radius: Float, violet: Color) {
    // Hold solid white well past the middle before falling away, so the bloom
    // reads as a lit disc rather than a faint smudge.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White,
                0.62f to Color.White,
                0.80f to Color.White.copy(alpha = 0.70f),
                1.00f to Color.White.copy(alpha = 0f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to violet.copy(alpha = 0.09f),
                1.00f to violet.copy(alpha = 0f),
            ),
            center = center,
            radius = radius * 0.52f,
        ),
        radius = radius * 0.52f,
        center = center,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F8FE, widthDp = 360, heightDp = 360)
@Composable
private fun RadarViewPreview() {
    FlyDropTheme {
        RadarView(
            devices = MockData.radarDevices,
            scanning = true,
            selectedUserId = MockData.ashley.id,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
