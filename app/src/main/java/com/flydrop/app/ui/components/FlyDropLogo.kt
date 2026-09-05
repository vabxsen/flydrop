package com.flydrop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * The FlyDrop wordmark: lowercase "flydrop" in heavy navy, followed by the
 * folded paper-plane glyph. [suffix] renders the lighter grey word that follows
 * it on the Nearby screen (`flydrop ✈ nearby™`).
 */
@Composable
fun FlyDropLogo(
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "flydrop",
            style = FlyDrop.type.logo,
            color = FlyDrop.colors.textPrimary,
        )
        PlaneGlyph()
        if (suffix != null) {
            Spacer(Modifier.width(1.dp))
            Text(
                text = suffixWithTrademark(suffix),
                style = FlyDrop.type.logoMuted,
                color = FlyDrop.colors.textTertiary,
            )
        }
    }
}

/** The solid folded-paper mark sitting to the right of the wordmark. */
@Composable
private fun PlaneGlyph(modifier: Modifier = Modifier) {
    val color = FlyDrop.colors.logoPlane
    Canvas(modifier = modifier.size(width = 20.dp, height = 15.dp)) {
        val w = size.width
        val h = size.height
        // Upper wing: a swept parallelogram.
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w * 0.52f, h * 0.52f)
                lineTo(w * 0.12f, h * 0.52f)
                close()
            },
            color = color,
        )
        // Lower fin, slightly inset, giving the folded look.
        drawPath(
            path = Path().apply {
                moveTo(w * 0.30f, h * 0.62f)
                lineTo(w * 0.86f, h * 0.62f)
                lineTo(w * 0.44f, h)
                close()
            },
            color = color,
        )
    }
}

private fun suffixWithTrademark(suffix: String): AnnotatedString = buildAnnotatedString {
    append(suffix)
    withStyle(SpanStyle(fontSize = 9.sp, baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript)) {
        append("TM")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE3FBFD, widthDp = 320)
@Composable
private fun FlyDropLogoPreview() {
    FlyDropTheme {
        Row(
            modifier = Modifier.size(width = 320.dp, height = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))
            FlyDropLogo(suffix = "nearby")
        }
    }
}
