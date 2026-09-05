package com.flydrop.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.profile.FlyIdRules
import com.flydrop.app.ui.components.Avatar
import com.flydrop.app.ui.components.Chevron
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme

/**
 * "Find by FlyDrop ID" - the card in the hero where Flydrop Web used to sit.
 *
 * FlyDrop IDs are globally unique, so this is an exact lookup rather than a
 * search: one id names at most one account. The result appears in place, under
 * the field, and opens a transfer when tapped.
 */
@Composable
fun FlyIdSearchCard(
    state: UserSearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onOpenResult: (FlyUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.card,
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = "Find by FlyDrop ID",
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Every FlyDrop ID belongs to one person.",
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))

            SearchField(
                state = state,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onClear = onClear,
            )

            AnimatedVisibility(
                visible = state.result != null || state.message != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150)),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    val found = state.result
                    if (found != null) {
                        SearchResultRow(user = found, onClick = { onOpenResult(found) })
                    } else {
                        Text(
                            text = state.message.orEmpty(),
                            style = FlyDrop.type.metadata,
                            color = FlyDrop.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    state: UserSearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    val shape = FlyDrop.shapes.tile

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(colors.paleTile, shape)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = FlyIdRules.PREFIX,
                style = FlyDrop.type.cardTitle,
                color = colors.textTertiary,
            )
            Box(modifier = Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        text = "username",
                        style = FlyDrop.type.cardTitle,
                        color = colors.textTertiary,
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    enabled = !state.searching,
                    singleLine = true,
                    textStyle = FlyDrop.type.cardTitle.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.violet),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.query.isNotEmpty() && !state.searching) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(FlyDrop.shapes.chip)
                        .clickable(onClick = onClear)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "×",
                        style = FlyDrop.type.cardTitle,
                        color = colors.textTertiary,
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(FlyDrop.shapes.button)
                .background(
                    if (state.canSearch) colors.violet else colors.violetSoft,
                    FlyDrop.shapes.button,
                )
                .clickable(enabled = state.canSearch, onClick = onSearch)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            if (state.searching) {
                CircularProgressIndicator(
                    color = colors.violet,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    imageVector = FlyDropIcons.Send,
                    contentDescription = "Search",
                    tint = if (state.canSearch) Color.White else colors.violet,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** The found account. Tapping opens a transfer, as a favourite friend does. */
@Composable
private fun SearchResultRow(
    user: FlyUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FlyDrop.shapes.smallCard)
            .background(FlyDrop.colors.tealSoft, FlyDrop.shapes.smallCard)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Avatar(seed = user.avatarSeed, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.flyId,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Tap to send files",
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textSecondary,
            )
        }
        Chevron()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFDCF2F1, widthDp = 360)
@Composable
private fun FlyIdSearchCardPreview() {
    FlyDropTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlyIdSearchCard(
                state = UserSearchState(query = "lucas"),
                onQueryChange = {},
                onSearch = {},
                onClear = {},
                onOpenResult = {},
            )
            FlyIdSearchCard(
                state = UserSearchState(
                    query = "lucas",
                    result = FlyUser(
                        id = "uid",
                        name = "fly#lucas",
                        flyId = "fly#lucas",
                        avatarSeed = 7,
                    ),
                ),
                onQueryChange = {},
                onSearch = {},
                onClear = {},
                onOpenResult = {},
            )
            FlyIdSearchCard(
                state = UserSearchState(
                    query = "nobody",
                    message = "No one is using that FlyDrop ID.",
                ),
                onQueryChange = {},
                onSearch = {},
                onClear = {},
                onOpenResult = {},
            )
        }
    }
}
