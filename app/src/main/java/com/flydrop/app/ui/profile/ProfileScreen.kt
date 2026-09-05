package com.flydrop.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.flydrop.app.data.MockData
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.profile.FlyIdRules
import com.flydrop.app.ui.components.Avatar
import com.flydrop.app.ui.components.FlyDropIcons
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop
import com.flydrop.app.ui.theme.FlyDropTheme
import kotlinx.coroutines.delay

/**
 * Profile.
 *
 * The reference does not show this screen, so it is assembled from the same
 * parts as the others: the aqua hero, a white sheet, and the app's card
 * treatment. It carries the signed-in account, the FlyDrop ID that account is
 * known by, and the way back out of it.
 */
@Composable
fun ProfileScreen(
    user: FlyUser,
    modifier: Modifier = Modifier,
    signedIn: Boolean = false,
    flyIdState: ProfileUiState = ProfileUiState(),
    onEditFlyId: () -> Unit = {},
    onFlyIdInputChange: (String) -> Unit = {},
    onConfirmFlyId: () -> Unit = {},
    onDismissFlyIdEditor: () -> Unit = {},
    onDismissFlyIdMessage: () -> Unit = {},
    onEditAvatar: () -> Unit = {},
    onChooseAvatar: () -> Unit = {},
    onRemoveAvatar: () -> Unit = {},
    onDismissAvatarSheet: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onSignOut: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = FlyDrop.colors
    val dimens = FlyDrop.dimens

    // The confirmation has been read by the time it has been on screen this
    // long, and leaving it up would make it read as part of the card stack.
    LaunchedEffect(flyIdState.message) {
        if (flyIdState.message != null) {
            delay(MESSAGE_DURATION_MS)
            onDismissFlyIdMessage()
        }
    }

    // The stored id when there is one, otherwise the id derived from the
    // account, so the card is never empty while Firestore is being read.
    val flyId = flyIdState.flyId ?: user.flyId

    // Aqua behind everything, so the white sheet's rounded top corners have
    // something to reveal - the same lip Home and Nearby use.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.heroAqua),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.heroAqua)
                .padding(top = contentPadding.calculateTopPadding())
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            EditableAvatar(
                seed = user.avatarSeed,
                photo = flyIdState.avatar,
                busy = flyIdState.avatarBusy,
                onClick = onEditAvatar,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = user.name,
                style = FlyDrop.type.screenTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = user.email ?: flyId,
                style = FlyDrop.type.label,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(FlyDrop.shapes.sheet)
                .background(colors.surface)
                .padding(horizontal = dimens.screenPadding)
                .padding(
                    top = dimens.panelTopPadding,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            FlyIdCard(
                flyId = flyId,
                canEdit = flyIdState.canEdit,
                changed = flyIdState.changed,
                onEdit = onEditFlyId,
            )
            Spacer(Modifier.height(dimens.cardGap))
            InfoCard(
                label = "Account",
                value = if (signedIn) "Signed in with Google" else "Not signed in",
            )
            Spacer(Modifier.height(dimens.cardGap))
            NavigationRow(
                label = "Settings",
                icon = FlyDropIcons.Settings,
                onClick = onOpenSettings,
            )
            Spacer(Modifier.height(dimens.cardGap))
            NavigationRow(
                label = "About",
                onClick = onOpenAbout,
            )

            AnimatedVisibility(
                visible = flyIdState.message != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Column {
                    Spacer(Modifier.height(dimens.cardGap))
                    Text(
                        text = flyIdState.message.orEmpty(),
                        style = FlyDrop.type.secondary,
                        color = if (flyIdState.messageIsError) ErrorRed else colors.teal,
                    )
                }
            }

            Spacer(Modifier.height(dimens.sectionGap))
            AccountActionButton(
                signedIn = signedIn,
                onClick = onSignOut,
            )
        }
    }

    if (flyIdState.editorOpen) {
        FlyIdEditorDialog(
            state = flyIdState,
            onInputChange = onFlyIdInputChange,
            onConfirm = onConfirmFlyId,
            onDismiss = onDismissFlyIdEditor,
        )
    }

    if (flyIdState.avatarSheetOpen) {
        AvatarChoiceDialog(
            hasPhoto = flyIdState.avatar != null,
            onChoose = onChooseAvatar,
            onRemove = onRemoveAvatar,
            onDismiss = onDismissAvatarSheet,
        )
    }
}

/**
 * The profile photo with its edit affordance: a violet camera badge tucked into
 * the lower-right of the disc, ringed in the hero colour so it reads as sitting
 * on top of the avatar rather than punched out of it.
 */
@Composable
private fun EditableAvatar(
    seed: Int,
    photo: ImageBitmap?,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    val colors = FlyDrop.colors
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Avatar(seed = seed, size = size, photo = photo)

        // Dimmed while a pick is being decoded, so the badge's spinner reads as
        // being about this avatar.
        if (busy) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.heroAqua, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(colors.violet, CircleShape)
                .clickable(enabled = !busy, onClick = onClick)
                .semantics { contentDescription = "Change profile photo" },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp),
                )
            } else {
                Icon(
                    imageVector = FlyDropIcons.Camera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/**
 * Picking a photo and clearing it are one decision, so they are offered
 * together rather than the badge silently launching the picker — clearing has
 * no other home, and a tap that opens a system picker with no warning is worse
 * than a tap that explains itself.
 */
@Composable
private fun AvatarChoiceDialog(
    hasPhoto: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FlyDrop.colors
    Dialog(onDismissRequest = onDismiss) {
        SoftCard(shape = FlyDrop.shapes.largeCard, elevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Profile photo",
                    style = FlyDrop.type.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Kept on this device only. It is not uploaded, so it " +
                        "does not follow you to another phone.",
                    style = FlyDrop.type.secondary,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(16.dp))

                ChoiceRow(
                    label = if (hasPhoto) "Choose a different photo" else "Choose a photo",
                    onClick = onChoose,
                )
                if (hasPhoto) {
                    Spacer(Modifier.height(FlyDrop.dimens.cardGap))
                    ChoiceRow(
                        label = "Use my generated avatar",
                        onClick = onRemove,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    DialogButton(
                        label = "Cancel",
                        enabled = true,
                        filled = false,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = FlyDrop.shapes.tile
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FlyDrop.colors.paleTile, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FlyDrop.type.cardTitle,
            color = FlyDrop.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = FlyDropIcons.ChevronRight,
            contentDescription = null,
            tint = FlyDrop.colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The id card, with its one-time change offered while it is still available.
 *
 * The caption is the whole warning: the change cannot be taken back, so it is
 * stated on the card rather than only inside the dialog the user has to open
 * to see it.
 */
@Composable
private fun FlyIdCard(
    flyId: String,
    canEdit: Boolean,
    changed: Boolean,
    onEdit: () -> Unit,
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FlyDrop ID",
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textTertiary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = flyId,
                    style = FlyDrop.type.cardTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                val caption = when {
                    changed -> "Changed once - this is now permanent."
                    canEdit -> "You can change this once."
                    else -> null
                }
                if (caption != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = caption,
                        style = FlyDrop.type.metadata,
                        color = FlyDrop.colors.textTertiary,
                    )
                }
            }

            if (canEdit) {
                Spacer(Modifier.width(10.dp))
                ChangeIdButton(onClick = onEdit)
            }
        }
    }
}

@Composable
private fun ChangeIdButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = FlyDrop.shapes.chip
    Text(
        text = "Change",
        style = FlyDrop.type.chipLabel,
        color = FlyDrop.colors.violet,
        modifier = modifier
            .clip(shape)
            .background(FlyDrop.colors.violetSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * Asks for the new id, and is the last chance to back out.
 *
 * The field holds only the part after `fly#`; the prefix sits beside it as
 * static text so it cannot be edited away or typed twice. Whatever is typed is
 * normalised on the way in by the view model, so the field always shows exactly
 * the id that would be claimed.
 */
@Composable
private fun FlyIdEditorDialog(
    state: ProfileUiState,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FlyDrop.colors
    val focusRequester = remember { FocusRequester() }
    val canSave = state.input.isNotEmpty() && state.inputError == null && !state.saving
    val editorError = state.inputError ?: state.submissionError

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        SoftCard(shape = FlyDrop.shapes.largeCard, elevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Choose your FlyDrop ID",
                    style = FlyDrop.type.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This is how people find you. It can only be changed once, " +
                        "and no two people can share one.",
                    style = FlyDrop.type.secondary,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(16.dp))

                FlyIdField(
                    value = state.input,
                    enabled = !state.saving,
                    hasError = state.inputError != null,
                    focusRequester = focusRequester,
                    onValueChange = onInputChange,
                    onSubmit = { if (canSave) onConfirm() },
                )

                AnimatedVisibility(
                    visible = editorError != null,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(140)),
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = editorError.orEmpty(),
                            style = FlyDrop.type.metadata,
                            color = ErrorRed,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    DialogButton(
                        label = "Cancel",
                        enabled = !state.saving,
                        filled = false,
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(10.dp))
                    DialogButton(
                        label = if (state.submissionError != null) "Retry" else "Save",
                        enabled = canSave,
                        filled = true,
                        loading = state.saving,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlyIdField(
    value: String,
    enabled: Boolean,
    hasError: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FlyDrop.colors
    val shape = FlyDrop.shapes.tile

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.paleTile, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = FlyIdRules.PREFIX,
            style = FlyDrop.type.cardTitle,
            color = colors.textTertiary,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "yourname",
                    style = FlyDrop.type.cardTitle,
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = FlyDrop.type.cardTitle.copy(
                    color = if (hasError) ErrorRed else colors.textPrimary,
                ),
                cursorBrush = SolidColor(colors.violet),
                keyboardOptions = KeyboardOptions(
                    // The id is lowercase and never a word, so neither
                    // autocorrect nor auto-capitalisation has anything useful
                    // to add - both would only fight the normalisation.
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

@Composable
private fun NavigationRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FlyDrop.colors.violet,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = label,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = FlyDropIcons.ChevronRight,
                contentDescription = null,
                tint = FlyDrop.colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = FlyDrop.shapes.smallCard,
        elevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = FlyDrop.type.secondary,
                color = FlyDrop.colors.textTertiary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = FlyDrop.type.cardTitle,
                color = FlyDrop.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun AccountActionButton(
    signedIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FlyDrop.shapes.button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FlyDrop.dimens.actionButtonHeight)
            .clip(shape)
            .background(FlyDrop.colors.violetSoft, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FlyDropIcons.ArrowLeft,
            contentDescription = null,
            tint = FlyDrop.colors.violet,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (signedIn) "Sign out" else "Sign in",
            style = FlyDrop.type.buttonLabel,
            color = FlyDrop.colors.violet,
        )
    }
}

/** Long enough to read the confirmation, short enough not to become furniture. */
private const val MESSAGE_DURATION_MS = 6_000L

/** Matches the sign-in screen's error tone. */
private val ErrorRed = Color(0xFFD1453B)

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenPreview() {
    FlyDropTheme {
        ProfileScreen(
            user = MockData.currentUser.copy(email = "lucas@example.com"),
            signedIn = true,
            flyIdState = ProfileUiState(flyId = "fly#lucas", canEdit = true),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenIdChangedPreview() {
    FlyDropTheme {
        ProfileScreen(
            user = MockData.currentUser.copy(email = "lucas@example.com"),
            signedIn = true,
            flyIdState = ProfileUiState(flyId = "fly#lucas", canEdit = false, changed = true),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenEditorPreview() {
    FlyDropTheme {
        ProfileScreen(
            user = MockData.currentUser.copy(email = "lucas@example.com"),
            signedIn = true,
            flyIdState = ProfileUiState(
                flyId = "fly#a1b2c3",
                canEdit = true,
                editorOpen = true,
                input = "lucas",
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenAvatarSheetPreview() {
    FlyDropTheme {
        ProfileScreen(
            user = MockData.currentUser.copy(email = "lucas@example.com"),
            signedIn = true,
            flyIdState = ProfileUiState(
                flyId = "fly#lucas",
                canEdit = true,
                avatarSheetOpen = true,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ProfileScreenSignedOutPreview() {
    FlyDropTheme {
        ProfileScreen(user = MockData.currentUser)
    }
}
