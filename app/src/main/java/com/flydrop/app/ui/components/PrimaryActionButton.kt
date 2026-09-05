package com.flydrop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * The pair of solid pill buttons in the profile card. Deliberately not a
 * Material [androidx.compose.material3.Button]: the reference uses a taller box,
 * a much larger corner radius and no elevation at all.
 */
@Composable
fun PrimaryActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "actionButtonScale",
    )
    val haptics = LocalHapticFeedback.current
    val shape = FlyDrop.shapes.button

    Row(
        modifier = modifier
            .height(FlyDrop.dimens.actionButtonHeight)
            .scale(scale)
            .clip(shape)
            .background(containerColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = FlyDrop.type.buttonLabel,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 340)
@Composable
private fun PrimaryActionButtonPreview() {
    FlyDropTheme {
        Row(
            modifier = Modifier.size(width = 340.dp, height = 74.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryActionButton(
                label = "Send File",
                icon = FlyDropIcons.Send,
                containerColor = FlyDrop.colors.violet,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            PrimaryActionButton(
                label = "Receive File",
                icon = FlyDropIcons.FileReceive,
                containerColor = FlyDrop.colors.teal,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
