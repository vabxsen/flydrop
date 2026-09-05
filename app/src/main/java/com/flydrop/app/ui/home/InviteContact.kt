package com.flydrop.app.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.ui.components.DialogTextButton
import com.flydrop.app.ui.components.SoftCard
import com.flydrop.app.ui.theme.FlyDrop

/**
 * The invite prompt shown when a contact row is tapped.
 *
 * Deliberately small: it offers one action, and says up front that the message
 * is handed to the messaging app rather than sent, so nobody taps expecting a
 * text to go out on its own.
 */
@Composable
fun InviteContactDialog(
    contact: FlyUser,
    onInvite: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        SoftCard(shape = FlyDrop.shapes.largeCard, elevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Invite to app",
                    style = FlyDrop.type.sectionTitle,
                    color = FlyDrop.colors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (contact.phoneNumber != null) {
                        "Open a message to ${contact.name} with a link to download FlyDrop. " +
                            "You still choose when to send it."
                    } else {
                        "${contact.name} has no phone number saved, so the message will " +
                            "open without a recipient for you to pick one."
                    },
                    style = FlyDrop.type.secondary,
                    color = FlyDrop.colors.textSecondary,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    DialogTextButton(label = "Not now", filled = false, onClick = onDismiss)
                    Spacer(Modifier.width(10.dp))
                    DialogTextButton(label = "Invite", filled = true, onClick = onInvite)
                }
            }
        }
    }
}

/**
 * Hands the invite to the phone's default messaging app, pre-addressed and
 * pre-filled.
 *
 * `ACTION_SENDTO` on an `smsto:` URI opens the composer; it never sends. Sending
 * silently would need SEND_SMS, a permission this app does not ask for and does
 * not want - a message going out with no confirmation is not something a file
 * sharing app should be able to do.
 *
 * Returns false when the phone has no app that can handle it, so the caller can
 * say so rather than appearing to do nothing.
 */
fun sendInvite(context: Context, contact: FlyUser, downloadUrl: String): Boolean {
    val body = "I'm using FlyDrop to share files. Get it here: $downloadUrl"
    // An empty recipient is valid here: the messaging app opens on its
    // recipient picker instead of refusing the intent.
    val target = "smsto:${contact.phoneNumber.orEmpty()}"

    val intent = Intent(Intent.ACTION_SENDTO, target.toUri()).apply {
        putExtra("sms_body", body)
    }

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
