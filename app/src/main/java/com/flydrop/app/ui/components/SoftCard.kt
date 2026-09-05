package com.flydrop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDrop

/**
 * The one surface treatment used by every card in the app: white fill, a
 * hairline border, and a whisper of shadow.
 *
 * The reference has almost no elevation. Separation comes from the border and a
 * very low-opacity ambient shadow, so cards read as soft and floating rather
 * than lifted. Material [androidx.compose.material3.Card] defaults are far
 * heavier than this, which is why the app does not use them.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = FlyDrop.colors.surface,
    borderColor: Color = FlyDrop.colors.hairline,
    elevation: Dp = 5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "softCardScale",
    )
    val indication = ripple(color = FlyDrop.colors.violet)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = ShadowTint,
                spotColor = ShadowTint,
            )
            .background(color = color, shape = shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = indication,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

/**
 * Very cool-toned, very transparent. A neutral black shadow reads as grey dirt
 * against the pale lavender backgrounds in the reference.
 */
private val ShadowTint = Color(0x2A5B6BA8)

