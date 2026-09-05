package com.flydrop.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-built outline icons. The reference uses an even, fairly thin stroke with
 * round caps throughout. Material stock icons are visibly heavier and more
 * filled, which pulls the screens off-design, so these are drawn to match.
 */
private fun strokeIcon(
    name: String,
    strokeWidth: Float = 1.7f,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}.build()

object FlyDropIcons {

    val Send: ImageVector by lazy {
        strokeIcon("Send") {
            moveTo(21.4f, 3.0f); lineTo(2.8f, 10.6f); lineTo(10.6f, 13.9f)
            lineTo(13.9f, 21.4f); close()
            moveTo(21.4f, 3.0f); lineTo(10.6f, 13.9f)
        }
    }

    val FileReceive: ImageVector by lazy {
        strokeIcon("FileReceive") {
            moveTo(13.6f, 2.9f); horizontalLineTo(7.5f)
            curveTo(6.3f, 2.9f, 5.3f, 3.9f, 5.3f, 5.1f)
            verticalLineTo(18.9f)
            curveTo(5.3f, 20.1f, 6.3f, 21.1f, 7.5f, 21.1f)
            horizontalLineTo(16.5f)
            curveTo(17.7f, 21.1f, 18.7f, 20.1f, 18.7f, 18.9f)
            verticalLineTo(8.0f); close()
            moveTo(13.6f, 2.9f); verticalLineTo(8.0f); horizontalLineTo(18.7f)
            moveTo(12.0f, 13.6f)
            curveTo(13.0f, 13.6f, 13.8f, 14.4f, 13.8f, 15.4f)
            curveTo(13.8f, 16.4f, 13.0f, 17.2f, 12.0f, 17.2f)
            curveTo(11.0f, 17.2f, 10.2f, 16.4f, 10.2f, 15.4f)
            curveTo(10.2f, 14.4f, 11.0f, 13.6f, 12.0f, 13.6f); close()
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            moveTo(6.2f, 9.4f)
            curveTo(6.2f, 6.2f, 8.8f, 3.6f, 12.0f, 3.6f)
            curveTo(15.2f, 3.6f, 17.8f, 6.2f, 17.8f, 9.4f)
            curveTo(17.8f, 13.6f, 18.9f, 15.4f, 20.1f, 16.6f)
            horizontalLineTo(3.9f)
            curveTo(5.1f, 15.4f, 6.2f, 13.6f, 6.2f, 9.4f); close()
            moveTo(9.9f, 19.4f)
            curveTo(10.2f, 20.4f, 11.0f, 21.0f, 12.0f, 21.0f)
            curveTo(13.0f, 21.0f, 13.8f, 20.4f, 14.1f, 19.4f)
        }
    }

    val ScanFrame: ImageVector by lazy {
        strokeIcon("ScanFrame") {
            moveTo(3.6f, 8.6f); verticalLineTo(6.1f)
            curveTo(3.6f, 4.7f, 4.7f, 3.6f, 6.1f, 3.6f); horizontalLineTo(8.6f)
            moveTo(15.4f, 3.6f); horizontalLineTo(17.9f)
            curveTo(19.3f, 3.6f, 20.4f, 4.7f, 20.4f, 6.1f); verticalLineTo(8.6f)
            moveTo(20.4f, 15.4f); verticalLineTo(17.9f)
            curveTo(20.4f, 19.3f, 19.3f, 20.4f, 17.9f, 20.4f); horizontalLineTo(15.4f)
            moveTo(8.6f, 20.4f); horizontalLineTo(6.1f)
            curveTo(4.7f, 20.4f, 3.6f, 19.3f, 3.6f, 17.9f); verticalLineTo(15.4f)
            moveTo(4.2f, 12.0f); horizontalLineTo(19.8f)
        }
    }

    val CloudTransfer: ImageVector by lazy {
        strokeIcon("CloudTransfer") {
            moveTo(7.6f, 18.4f); horizontalLineTo(16.6f)
            curveTo(18.7f, 18.4f, 20.4f, 16.7f, 20.4f, 14.6f)
            curveTo(20.4f, 12.7f, 19.0f, 11.1f, 17.2f, 10.8f)
            curveTo(16.7f, 7.9f, 14.2f, 5.7f, 11.2f, 5.7f)
            curveTo(8.4f, 5.7f, 6.0f, 7.6f, 5.3f, 10.2f)
            curveTo(4.0f, 10.8f, 3.1f, 12.2f, 3.1f, 13.8f)
            curveTo(3.1f, 16.3f, 5.1f, 18.4f, 7.6f, 18.4f); close()
            moveTo(11.8f, 10.6f); verticalLineTo(15.6f)
            moveTo(9.6f, 13.4f); lineTo(11.8f, 15.6f); lineTo(14.0f, 13.4f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight", strokeWidth = 1.9f) {
            moveTo(9.3f, 4.8f); lineTo(16.5f, 12.0f); lineTo(9.3f, 19.2f)
        }
    }

    val ArrowLeft: ImageVector by lazy {
        strokeIcon("ArrowLeft", strokeWidth = 1.9f) {
            moveTo(20.2f, 12.0f); horizontalLineTo(3.8f)
            moveTo(10.4f, 5.2f); lineTo(3.6f, 12.0f); lineTo(10.4f, 18.8f)
        }
    }

    val Home: ImageVector by lazy {
        strokeIcon("Home") {
            moveTo(3.5f, 10.2f); lineTo(12.0f, 3.5f); lineTo(20.5f, 10.2f)
            verticalLineTo(18.9f)
            curveTo(20.5f, 19.9f, 19.7f, 20.7f, 18.7f, 20.7f)
            horizontalLineTo(5.3f)
            curveTo(4.3f, 20.7f, 3.5f, 19.9f, 3.5f, 18.9f); close()
            moveTo(9.8f, 20.7f); verticalLineTo(16.0f)
            curveTo(9.8f, 15.4f, 10.3f, 14.9f, 10.9f, 14.9f)
            horizontalLineTo(13.1f)
            curveTo(13.7f, 14.9f, 14.2f, 15.4f, 14.2f, 16.0f)
            verticalLineTo(20.7f)
        }
    }

    val Globe: ImageVector by lazy {
        strokeIcon("Globe") {
            moveTo(12.0f, 3.3f)
            curveTo(16.8f, 3.3f, 20.7f, 7.2f, 20.7f, 12.0f)
            curveTo(20.7f, 16.8f, 16.8f, 20.7f, 12.0f, 20.7f)
            curveTo(7.2f, 20.7f, 3.3f, 16.8f, 3.3f, 12.0f)
            curveTo(3.3f, 7.2f, 7.2f, 3.3f, 12.0f, 3.3f); close()
            moveTo(3.5f, 12.0f); horizontalLineTo(20.5f)
            moveTo(12.0f, 3.3f)
            curveTo(14.2f, 5.7f, 15.3f, 8.7f, 15.3f, 12.0f)
            curveTo(15.3f, 15.3f, 14.2f, 18.3f, 12.0f, 20.7f)
            curveTo(9.8f, 18.3f, 8.7f, 15.3f, 8.7f, 12.0f)
            curveTo(8.7f, 8.7f, 9.8f, 5.7f, 12.0f, 3.3f); close()
        }
    }

    val Radar: ImageVector by lazy {
        strokeIcon("Radar") {
            moveTo(12.0f, 4.6f)
            curveTo(16.1f, 4.6f, 19.4f, 7.9f, 19.4f, 12.0f)
            curveTo(19.4f, 16.1f, 16.1f, 19.4f, 12.0f, 19.4f)
            curveTo(7.9f, 19.4f, 4.6f, 16.1f, 4.6f, 12.0f)
            curveTo(4.6f, 7.9f, 7.9f, 4.6f, 12.0f, 4.6f); close()
            moveTo(12.0f, 9.2f)
            curveTo(13.5f, 9.2f, 14.8f, 10.5f, 14.8f, 12.0f)
            curveTo(14.8f, 13.5f, 13.5f, 14.8f, 12.0f, 14.8f)
            curveTo(10.5f, 14.8f, 9.2f, 13.5f, 9.2f, 12.0f)
            curveTo(9.2f, 10.5f, 10.5f, 9.2f, 12.0f, 9.2f); close()
            moveTo(12.0f, 1.9f); verticalLineTo(4.3f)
            moveTo(12.0f, 19.7f); verticalLineTo(22.1f)
            moveTo(1.9f, 12.0f); horizontalLineTo(4.3f)
            moveTo(19.7f, 12.0f); horizontalLineTo(22.1f)
        }
    }

    val Person: ImageVector by lazy {
        strokeIcon("Person") {
            moveTo(12.0f, 3.5f)
            curveTo(14.1f, 3.5f, 15.9f, 5.3f, 15.9f, 7.4f)
            curveTo(15.9f, 9.5f, 14.1f, 11.3f, 12.0f, 11.3f)
            curveTo(9.9f, 11.3f, 8.1f, 9.5f, 8.1f, 7.4f)
            curveTo(8.1f, 5.3f, 9.9f, 3.5f, 12.0f, 3.5f); close()
            moveTo(4.9f, 20.5f)
            curveTo(4.9f, 16.7f, 8.1f, 14.4f, 12.0f, 14.4f)
            curveTo(15.9f, 14.4f, 19.1f, 16.7f, 19.1f, 20.5f)
        }
    }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus", strokeWidth = 2.1f) {
            moveTo(12.0f, 5.4f); verticalLineTo(18.6f)
            moveTo(5.4f, 12.0f); horizontalLineTo(18.6f)
        }
    }

    val Star: ImageVector by lazy {
        strokeIcon("Star", strokeWidth = 1.8f) {
            moveTo(12.0f, 3.2f)
            lineTo(14.7f, 8.7f)
            lineTo(20.8f, 9.6f)
            lineTo(16.4f, 13.9f)
            lineTo(17.4f, 20.0f)
            lineTo(12.0f, 17.1f)
            lineTo(6.6f, 20.0f)
            lineTo(7.6f, 13.9f)
            lineTo(3.2f, 9.6f)
            lineTo(9.3f, 8.7f)
            close()
        }
    }

    val ArrowDown: ImageVector by lazy {
        strokeIcon("ArrowDown", strokeWidth = 2.2f) {
            moveTo(12.0f, 5.6f); verticalLineTo(18.4f)
            moveTo(6.4f, 12.8f); lineTo(12.0f, 18.4f); lineTo(17.6f, 12.8f)
        }
    }

    val ArrowUp: ImageVector by lazy {
        strokeIcon("ArrowUp", strokeWidth = 2.2f) {
            moveTo(12.0f, 18.4f); verticalLineTo(5.6f)
            moveTo(6.4f, 11.2f); lineTo(12.0f, 5.6f); lineTo(17.6f, 11.2f)
        }
    }

    val Document: ImageVector by lazy {
        strokeIcon("Document") {
            moveTo(13.6f, 2.9f); horizontalLineTo(7.5f)
            curveTo(6.3f, 2.9f, 5.3f, 3.9f, 5.3f, 5.1f)
            verticalLineTo(18.9f)
            curveTo(5.3f, 20.1f, 6.3f, 21.1f, 7.5f, 21.1f)
            horizontalLineTo(16.5f)
            curveTo(17.7f, 21.1f, 18.7f, 20.1f, 18.7f, 18.9f)
            verticalLineTo(8.0f); close()
            moveTo(13.6f, 2.9f); verticalLineTo(8.0f); horizontalLineTo(18.7f)
        }
    }
}
