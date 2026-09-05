package com.flydrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDrop

/**
 * The app's dialog action: a filled violet pill for the confirming choice and a
 * plain label for the dismissing one.
 *
 * Shared so every dialog - choosing a FlyDrop ID, confirming a sign-out,
 * inviting a contact, turning the radios on - ends in the same pair of buttons
 * rather than each growing its own.
 */
@Composable
fun DialogTextButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = FlyDrop.colors
    val shape = FlyDrop.shapes.button
    val background = if (filled) colors.violet else Color.Transparent
    val content = if (filled) Color.White else colors.textSecondary

    Row(
        modifier = modifier
            .clip(shape)
            .background(
                color = if (filled && !enabled) colors.violetSoft else background,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = content,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = FlyDrop.type.buttonLabel,
            color = if (filled && !enabled) colors.violet else content,
        )
    }
}
