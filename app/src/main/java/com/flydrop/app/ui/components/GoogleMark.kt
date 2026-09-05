package com.flydrop.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flydrop.app.ui.theme.FlyDropTheme

// Official Google "G" path data. Each string stays on one line on purpose:
// splitting SVG path data across concatenated literals can silently fuse two
// numbers into one and quietly deform the mark.
private const val BLUE_PATH =
    "M45.12 24.5c0-1.56-.14-3.06-.4-4.5H24v8.51h11.84c-.51 2.75-2.06 5.08-4.39 6.64v5.52h7.11c4.16-3.83 6.56-9.47 6.56-16.17z"
private const val GREEN_PATH =
    "M24 46c5.94 0 10.92-1.97 14.56-5.33l-7.11-5.52c-1.97 1.32-4.49 2.1-7.45 2.1-5.73 0-10.58-3.87-12.31-9.07H4.34v5.7C7.96 41.07 15.4 46 24 46z"
private const val YELLOW_PATH =
    "M11.69 28.18C11.25 26.86 11 25.45 11 24s.25-2.86.69-4.18v-5.7H4.34C2.85 17.09 2 20.45 2 24s.85 6.91 2.34 9.88l7.35-5.7z"
private const val RED_PATH =
    "M24 10.75c3.23 0 6.13 1.11 8.41 3.29l6.31-6.31C34.91 4.18 29.93 2 24 2 15.4 2 7.96 6.93 4.34 14.12l7.35 5.7c1.73-5.2 6.58-9.07 12.31-9.07z"

/**
 * The four-colour Google "G".
 *
 * Kept out of [FlyDropIcons] because those are single-colour stroked glyphs
 * that get tinted at the call site; this one carries its own brand colours and
 * must never be recoloured, so it is drawn with [Image] rather than `Icon`.
 */
private val GoogleG: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleG",
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 48f,
        viewportHeight = 48f,
    )
        .addPath(addPathNodes(BLUE_PATH), fill = SolidColor(Color(0xFF4285F4)))
        .addPath(addPathNodes(GREEN_PATH), fill = SolidColor(Color(0xFF34A853)))
        .addPath(addPathNodes(YELLOW_PATH), fill = SolidColor(Color(0xFFFBBC05)))
        .addPath(addPathNodes(RED_PATH), fill = SolidColor(Color(0xFFEA4335)))
        .build()
}

@Composable
fun GoogleMark(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Image(
        imageVector = GoogleG,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GoogleMarkPreview() {
    FlyDropTheme {
        GoogleMark(size = 48.dp)
    }
}
