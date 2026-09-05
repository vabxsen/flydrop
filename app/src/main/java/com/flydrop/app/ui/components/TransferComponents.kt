package com.flydrop.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FileKind
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.TransferDirection
import com.flydrop.app.data.model.TransferFile
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * The large circular transfer visualisation: a faint full ring, a teal progress
 * arc sweeping clockwise from 12 o'clock, the two participants, and the file
 * badge travelling between them.
 */
@Composable
fun TransferCircle(
    peer: FlyUser,
    self: FlyUser,
    direction: TransferDirection,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "transferProgress",
    )

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // The participants scale with the circle rather than sitting at fixed
        // sizes, so the composition holds its reference proportions
        // (avatar 56, tile 46 and a 44 gap across a 206-wide circle) on any screen.
        val diameter = maxWidth
        val avatarSize = diameter * 0.272f
        val fileTileSize = diameter * 0.223f
        val avatarGap = diameter * 0.214f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = colors.radarRing,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawArc(
                color = colors.teal,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Faint inner ring plus the dashed arc above the avatars.
            val innerRadius = size.minDimension * 0.34f
            drawCircle(
                color = colors.radarRing.copy(alpha = 0.7f),
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawArc(
                color = colors.radarRing,
                startAngle = -145f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(
                    size.width / 2f - innerRadius * 1.32f,
                    size.height / 2f - innerRadius * 1.32f,
                ),
                size = Size(innerRadius * 2.64f, innerRadius * 2.64f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 11f)),
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-6).dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(avatarGap)) {
                    Avatar(seed = self.avatarSeed, size = avatarSize)
                    Avatar(seed = peer.avatarSeed, size = avatarSize)
                }
                // The teal file badge sits on top of, and between, both avatars.
                Box(
                    modifier = Modifier
                        .size(fileTileSize)
                        .clip(FlyDrop.shapes.fileTile)
                        .background(colors.teal, FlyDrop.shapes.fileTile),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FlyDropIcons.FileReceive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(fileTileSize * 0.48f),
                    )
                }
            }

            Spacer(Modifier.height(diameter * 0.087f))
            Text(
                text = if (direction == TransferDirection.Incoming) {
                    "Receiving file from"
                } else {
                    "Sending file to"
                },
                style = FlyDrop.type.secondary,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = peer.name.substringBefore(' '),
                style = FlyDrop.type.profileName,
                color = colors.textPrimary,
            )
        }
    }
}

/** The three-column Items / Size / Speed strip beneath the transfer circle. */
@Composable
fun TransferStatsCard(
    itemsLabel: String,
    sizeLabel: String,
    speedLabel: String,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.card,
        elevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlyDrop.dimens.statsCardHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn("Items", itemsLabel, Modifier.weight(1f))
            StatDivider()
            StatColumn("Size", sizeLabel, Modifier.weight(1f))
            StatDivider()
            StatColumn("Speed", speedLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = FlyDrop.type.secondary,
            color = FlyDrop.colors.textTertiary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = FlyDrop.type.statValue,
            color = FlyDrop.colors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(FlyDrop.colors.divider),
    )
}

/** One row in "File Details". */
@Composable
fun TransferFileRow(
    file: TransferFile,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlyDrop.dimens.fileRowHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeIcon(kind = file.kind, seed = file.name.hashCode())
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = file.sizeLabel(),
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                )
            }
            val progress = file.progress
            if (progress != null) {
                Spacer(Modifier.width(10.dp))
                CircularFileProgress(progress = progress)
            }
        }
    }
}

private fun TransferFile.sizeLabel(): String {
    val total = formatBytes(totalBytes)
    val done = transferredBytes?.let { formatBytes(it) }
    return if (done != null) "$done / $total" else total
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (mb >= 100) "${mb.toInt()} MB" else String.format("%.1f MB", mb)
}

/** Small violet ring showing per-file progress. */
@Composable
private fun CircularFileProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = FlyDrop.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "fileProgress",
    )
    Canvas(modifier = modifier.size(FlyDrop.dimens.fileProgress)) {
        val stroke = 3.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = colors.violetSoft,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = colors.violet,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * Leading tile for a file row: a soft photo-like tile for images, and a tinted
 * document glyph for everything else.
 */
@Composable
fun FileTypeIcon(kind: FileKind, seed: Int, modifier: Modifier = Modifier) {
    val size = FlyDrop.dimens.fileThumb
    val shape = FlyDrop.shapes.tile
    when (kind) {
        FileKind.Image -> Box(
            modifier = modifier
                .size(size)
                .clip(shape),
        ) {
            ImageThumbnail(seed = seed, modifier = Modifier.fillMaxSize())
        }

        else -> {
            val (tint, background) = when (kind) {
                FileKind.Pdf -> Color(0xFFE8453C) to Color(0xFFFDE9E8)
                FileKind.Archive -> Color(0xFF8A93AC) to Color(0xFFEFF1F6)
                else -> FlyDrop.colors.violet to FlyDrop.colors.violetSoft
            }
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape)
                    .background(background, shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FlyDropIcons.Document,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(23.dp),
                )
                if (kind != FileKind.Document) {
                    Text(
                        text = if (kind == FileKind.Pdf) "PDF" else "ZIP",
                        style = FlyDrop.type.metadata.copy(fontSize = 7.sp),
                        color = tint,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 11.dp),
                    )
                }
            }
        }
    }
}

/**
 * A stand-in for an image preview: a soft gradient with a horizon and a light
 * source, which reads as a photograph at thumbnail size without shipping one.
 */
@Composable
private fun ImageThumbnail(seed: Int, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFF6FB6E8), Color(0xFF2B6FA8)),
        listOf(Color(0xFFE9A7B4), Color(0xFF9B5F7C)),
        listOf(Color(0xFF8FD3C4), Color(0xFF35786F)),
        listOf(Color(0xFFF0C48A), Color(0xFFB1713C)),
    )
    val palette = palettes[Math.floorMod(seed, palettes.size)]
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(palette),
            size = size,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = size.minDimension * 0.14f,
            center = Offset(size.width * 0.72f, size.height * 0.26f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(-size.width * 0.25f, size.height * 0.55f),
            size = Size(size.width * 1.5f, size.height * 1.1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFEFEFE, widthDp = 360)
@Composable
private fun TransferComponentsPreview() {
    FlyDropTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TransferStatsCard(itemsLabel = "5 to 12", sizeLabel = "812 MB", speedLabel = "21.3 MB/s")
            MockData.transferFiles.forEach { TransferFileRow(file = it) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFEFEFE, widthDp = 340, heightDp = 340)
@Composable
private fun TransferCirclePreview() {
    FlyDropTheme {
        TransferCircle(
            peer = MockData.ashley,
            self = MockData.currentUser,
            direction = TransferDirection.Incoming,
            progress = 0.7f,
            modifier = Modifier.padding(20.dp),
        )
    }
}
