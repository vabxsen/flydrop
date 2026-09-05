package com.flydrop.app.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.MockData
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SectionHeader
import com.flydrop.app.ui.components.TransferCircle
import com.flydrop.app.ui.components.TransferFileRow
import com.flydrop.app.ui.components.TransferStatsCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

private const val TransferCircleWidthFraction = 0.58f

/**
 * File Transfer: a centred title over the circular progress visualisation, the
 * three-column stats strip, and the per-file list.
 */
@Composable
fun TransferScreen(
    state: TransferUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dimens = FlyDrop.dimens
    val transfer = state.transfer

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FlyDrop.colors.transferBackground),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item(key = "header") {
            TransferTopBar(onBack = onBack)
        }

        item(key = "circle") {
            // A width fraction rather than fixed insets, so the circle keeps the
            // reference proportion (206 across a 356-wide screen) at any size.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TransferCircle(
                    peer = transfer.peer,
                    self = MockData.currentUser,
                    direction = transfer.direction,
                    progress = transfer.progress,
                    modifier = Modifier.fillMaxWidth(TransferCircleWidthFraction),
                )
            }
            Spacer(Modifier.height(33.dp))
        }

        item(key = "stats") {
            TransferStatsCard(
                itemsLabel = transfer.itemsLabel,
                sizeLabel = transfer.totalSizeLabel,
                speedLabel = transfer.speedLabel,
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(dimens.sectionGap))
            SectionHeader(
                title = "File Details",
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(12.dp))
        }

        items(transfer.files, key = { it.id }) { file ->
            TransferFileRow(
                file = file,
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
            Spacer(Modifier.height(dimens.cardGap))
        }
    }
}

/**
 * Back affordance on the left with the title optically centred on the screen,
 * as in the reference. The back button is overlaid rather than laid out in a
 * row so the title stays centred regardless of its width.
 */
@Composable
private fun TransferTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = FlyDrop.dimens.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "File Transfer",
            style = FlyDrop.type.screenTitle,
            color = FlyDrop.colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = FlyDropIcons.ArrowLeft,
                contentDescription = "Back",
                tint = FlyDrop.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 860)
@Composable
private fun TransferScreenPreview() {
    FlyDropTheme {
        TransferScreen(state = TransferUiState(), onBack = {})
    }
}
