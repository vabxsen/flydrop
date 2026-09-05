package com.flydrop.app.data.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.flydrop.app.data.model.FlyUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Why sign-in cannot run, when it cannot. */
enum class AuthUnavailableReason {
    /** google-services.json is absent, so FirebaseApp never initialised. */
    FirebaseNotConfigured,

    /** Firebase is present but the OAuth web client id is missing from it. */
    MissingWebClientId,
}

@Immutable
sealed interface SignInResult {
    data class Success(val user: FlyUser) : SignInResult

    /** The user dismissed the sheet, or had no account to offer. Not an error. */
    data object Cancelled : SignInResult

    data class Failure(val message: String) : SignInResult
}

/**
 * Google sign-in through Credential Manager, exchanged for a Firebase session.
 *
 * Everything here degrades rather than crashes when Firebase is not configured:
 * [unavailableReason] is non-null in that case and the UI explains what to add,
 * so the app still builds and runs on a machine with no Firebase project.
 */
class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    }.getOrNull()

    /**
     * Generated into resources by the Google Services plugin. Resolved by name
     * rather than through `R.string` so this compiles when the plugin has not
     * been applied — which is the entire point of the conditional `apply` in
     * app/build.gradle.kts. Lint's preference for `R.string` cannot be followed
     * here because the symbol does not exist until Firebase is configured.
     */
    @SuppressLint("DiscouragedApi")
    private val webClientId: String? = runCatching {
        val id = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (id == 0) null else context.getString(id)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private val credentialManager by lazy { CredentialManager.create(context) }

    val unavailableReason: AuthUnavailableReason?
        get() = when {
            auth == null -> AuthUnavailableReason.FirebaseNotConfigured
            webClientId == null -> AuthUnavailableReason.MissingWebClientId
            else -> null
        }

    val currentUser: FlyUser?
        get() = auth?.currentUser?.toFlyUser()

    /** Emits the signed-in user, or null, whenever the Firebase session changes. */
    fun authState(): Flow<FlyUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toFlyUser()) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    /**
     * Credential Manager needs an Activity to host its bottom sheet, so the
     * caller passes one in rather than this holding onto a context.
     */
    suspend fun signInWithGoogle(activity: Activity): SignInResult {
        val firebaseAuth = auth ?: return SignInResult.Failure(
            "Firebase is not configured. Add app/google-services.json.",
        )
        val clientId = webClientId ?: return SignInResult.Failure(
            "No OAuth web client id found. Enable Google sign-in in Firebase and " +
                "re-download google-services.json.",
        )

        // Ask for an already-authorised account first: returning users get a
        // one-tap sheet. Only fall back to the full account picker if there is
        // nothing to offer, which is what NoCredentialException means here.
        val idToken = try {
            requestGoogleIdToken(activity, clientId, filterByAuthorizedAccounts = true)
        } catch (_: NoCredentialException) {
            try {
                requestGoogleIdToken(activity, clientId, filterByAuthorizedAccounts = false)
            } catch (_: NoCredentialException) {
                return SignInResult.Failure(
                    "No Google account available on this device.",
                )
            } catch (_: GetCredentialCancellationException) {
                return SignInResult.Cancelled
            } catch (e: GetCredentialException) {
                return SignInResult.Failure(e.friendlyMessage())
            }
        } catch (_: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: GetCredentialException) {
            return SignInResult.Failure(e.friendlyMessage())
        } ?: return SignInResult.Failure("Google did not return an ID token.")

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user?.toFlyUser()
                ?: return SignInResult.Failure("Firebase returned no user.")
            SignInResult.Success(user)
        } catch (e: Exception) {
            SignInResult.Failure(e.message ?: "Firebase sign-in failed.")
        }
    }

    private suspend fun requestGoogleIdToken(
        activity: Activity,
        clientId: String,
        filterByAuthorizedAccounts: Boolean,
    ): String? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        return if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            null
        }
    }

    suspend fun signOut() {
        auth?.signOut()
        // Without this the next sign-in silently auto-selects the same account.
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

private fun GetCredentialException.friendlyMessage(): String =
    message ?: "Google sign-in failed (${this::class.simpleName})."

/**
 * Maps a Firebase account onto the app's own user model. [avatarSeed] is
 * derived from the uid so a given account always draws the same avatar.
 */
private fun FirebaseUser.toFlyUser(): FlyUser = FlyUser(
    id = uid,
    name = displayName?.takeIf { it.isNotBlank() }
        ?: email?.substringBefore('@')
        ?: "FlyDrop User",
    flyId = "fly#" + uid.takeLast(6).uppercase(),
    avatarSeed = uid.hashCode(),
    isFriend = false,
    email = email,
    photoUrl = photoUrl?.toString(),
)

/**
 * Minimal Task-to-coroutine bridge, so the project does not need
 * kotlinx-coroutines-play-services for the single call that awaits a Task.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
