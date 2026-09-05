package com.flydrop.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.RadarDevice
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
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

/** One full turn of the devices around the rings. Slow enough to read as drift. */
private const val ORBIT_PERIOD_MS = 48_000

/** The ambient dots drift slower still, so the two layers never lock together. */
private const val DOT_ORBIT_PERIOD_MS = 90_000

/** One turn of the sweeping beam. */
private const val SWEEP_PERIOD_MS = 4_000

/** How long a device takes to travel between its orbit and the centre. */
private const val CONNECT_MS = 460

/** The size a device grows to once it is centred as the chosen recipient. */
private val ConnectedAvatarSize = 74.dp

/**
 * The circular discovery visualisation: thin concentric rings, a soft glowing
 * centre, a radar beam sweeping around it, scattered signal dots, and the
 * devices drifting slowly along the rings.
 *
 * Selecting a device flies it into the centre, where it reads as the chosen
 * recipient; the violet "you" ring fades out behind it and the remaining
 * devices keep orbiting. Selecting it again sends it back to its ring.
 *
 * Positions are polar (angle + radius fraction) and resolved against the
 * measured size, so the whole composition scales with the screen instead of
 * being pinned to fixed coordinates. Every animated value is read in the
 * layout or draw phase rather than in composition, so the orbit and the sweep
 * run without recomposing the radar each frame.
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
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarPulse",
    )
    // Gentle breathing of the central glow.
    val breathe = transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radarBreathe",
    )
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarSweep",
    )
    val orbit = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ORBIT_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarOrbit",
    )
    val dotOrbit = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DOT_ORBIT_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarDotOrbit",
    )

    // "You" only holds the centre while nothing has been brought into it.
    val youAlpha = animateFloatAsState(
        targetValue = if (selectedUserId == null) 1f else 0f,
        animationSpec = tween(durationMillis = CONNECT_MS, easing = FastOutSlowInEasing),
        label = "radarYouAlpha",
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

            drawSweep(center, radiusPx, sweep.value, colors.violet)

            if (scanning) {
                drawPulse(center, radiusPx, pulse.value, colors.violet)
            }

            drawCentreGlow(center, radiusPx * 0.36f * breathe.value, colors.violet)
        }

        radarDots.forEach { dot ->
            Box(
                modifier = Modifier
                    .polarOffset(
                        outerRadius = outerRadius,
                        radiusFraction = { dot.radiusFraction },
                        angleDegrees = { dot.angleDegrees + dotOrbit.value },
                    )
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
                color = colors.violet.copy(alpha = youAlpha.value),
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke),
            )
        }

        // The centred device is drawn last so it sits above the others.
        devices.sortedBy { it.user.id == selectedUserId }.forEach { device ->
            val connected = device.user.id == selectedUserId
            val radiusFraction = animateFloatAsState(
                targetValue = if (connected) 0f else device.radiusFraction,
                animationSpec = tween(durationMillis = CONNECT_MS, easing = FastOutSlowInEasing),
                label = "radarDeviceRadius",
            )
            val avatarSize = animateDpAsState(
                targetValue = if (connected) ConnectedAvatarSize else device.avatarSize.dp,
                animationSpec = tween(durationMillis = CONNECT_MS, easing = FastOutSlowInEasing),
                label = "radarDeviceSize",
            )

            Avatar(
                seed = device.user.avatarSeed,
                size = avatarSize.value,
                ringColor = if (connected) colors.violet else null,
                modifier = Modifier
                    .polarOffset(
                        outerRadius = outerRadius,
                        radiusFraction = { radiusFraction.value },
                        // A device on its way to the centre keeps drifting, so it
                        // curves in rather than snapping along a straight line.
                        angleDegrees = { device.angleDegrees + orbit.value },
                    )
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = if (connected) {
                            "${device.user.name}, selected"
                        } else {
                            device.user.name
                        }
                    }
                    .clickable { onDeviceClick(device) },
            )
        }
    }
}

/**
 * Offsets a centred child onto polar coordinates, with 0 degrees at 12 o'clock
 * and positive angles running clockwise.
 *
 * The position is supplied as lambdas and resolved in the layout phase, so an
 * animating angle or radius moves the child without recomposing it.
 */
private fun Modifier.polarOffset(
    outerRadius: Dp,
    radiusFraction: () -> Float,
    angleDegrees: () -> Float,
): Modifier = this.offset {
    val radius = outerRadius.toPx() * radiusFraction()
    val radians = Math.toRadians((angleDegrees() - 90f).toDouble())
    IntOffset(
        x = (radius * cos(radians)).roundToInt(),
        y = (radius * sin(radians)).roundToInt(),
    )
}

/** The beam: a soft wedge trailing behind its leading edge, like a radar sweep. */
private fun DrawScope.drawSweep(center: Offset, radiusPx: Float, degrees: Float, color: Color) {
    // sweepGradient starts at 3 o'clock, so the wedge is rotated into place with
    // the same -90 degree correction the device positions use.
    rotate(degrees = degrees - 90f, pivot = center) {
        drawCircle(
            brush = Brush.sweepGradient(
                0.00f to color.copy(alpha = 0f),
                0.10f to color.copy(alpha = 0.05f),
                0.17f to color.copy(alpha = 0.13f),
                0.18f to color.copy(alpha = 0f),
                1.00f to color.copy(alpha = 0f),
                center = center,
            ),
            radius = radiusPx,
            center = center,
        )
    }
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
