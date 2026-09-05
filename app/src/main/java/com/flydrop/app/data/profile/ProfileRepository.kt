package com.flydrop.app.data.profile

import android.content.Context
import androidx.compose.runtime.Immutable
import com.flydrop.app.data.await
import com.flydrop.app.data.model.FlyUser
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/** An account's FlyDrop ID, and whether its one permitted change has been spent. */
@Immutable
data class FlyProfile(
    val uid: String,
    val handle: String,
    /** True once the user has renamed themselves; the id is then final. */
    val handleChanged: Boolean,
) {
    val flyId: String get() = FlyIdRules.display(handle)
    val editable: Boolean get() = !handleChanged
}

/** Outcome of looking someone up by their FlyDrop ID. */
@Immutable
sealed interface FlyIdSearchResult {
    data class Found(val user: FlyUser) : FlyIdSearchResult

    /** The id is well formed, but nobody holds it. */
    data object NotFound : FlyIdSearchResult

    data class Invalid(val error: FlyIdError) : FlyIdSearchResult

    data class Failure(val message: String) : FlyIdSearchResult
}

@Immutable
sealed interface ClaimResult {
    data class Success(val profile: FlyProfile) : ClaimResult

    /** Someone else already holds the id. */
    data object Taken : ClaimResult

    /** The account has already used its single change. */
    data object AlreadyChanged : ClaimResult

    data class Invalid(val error: FlyIdError) : ClaimResult

    data class Failure(val message: String) : ClaimResult
}

/**
 * Stores the FlyDrop ID of the signed-in account, and enforces the two rules
 * around it: an id belongs to exactly one account, and an account may choose
 * its own id exactly once.
 *
 * Both rules are kept by the data layout rather than by this class alone:
 *
 *  - `flyIds/{handle}` is a reservation document naming its owner. Claiming an
 *    id creates that document inside a transaction that first checks it is
 *    absent, so two accounts racing for the same id cannot both win. Handles
 *    are stored lowercase (see [FlyIdRules]) so case cannot be used to claim
 *    an id twice, and reservations are never released - an id a user has held
 *    stays theirs, so an old id can never start pointing at a stranger.
 *  - `users/{uid}.handleChanged` flips false to true on the one change and is
 *    read inside the same transaction, so a second change cannot slip through
 *    from two devices at once.
 *
 * The matching Firestore security rules in firestore.rules enforce the same
 * two things on the server, where a modified client cannot reach around them.
 * This class is the cooperative half; the rules are the authoritative half.
 *
 * Everything degrades rather than crashes when Firebase is absent:
 * [isAvailable] is false, and the UI keeps showing the derived default id
 * without offering to change it.
 */
class ProfileRepository(context: Context) {

    private val firestore: FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()
    }.getOrNull()

    val isAvailable: Boolean get() = firestore != null

    /**
     * Emits the stored profile, and every later change to it, so a rename on
     * one device shows up on the others. A document that does not exist yet
     * reports the derived default, so the id area is never blank.
     *
     * A listener error emits nothing rather than falling back: losing the
     * connection is not evidence the user's id reverted, and showing them the
     * derived default at that point would be a lie they might act on.
     */
    fun observeProfile(uid: String): Flow<FlyProfile> {
        val db = firestore ?: return flowOf(defaultProfile(uid))
        return callbackFlow {
            val registration = db.userDocument(uid).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                trySend(snapshot.toProfile(uid) ?: defaultProfile(uid))
            }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Creates the account's profile on first sign-in, reserving its derived
     * default id so nobody else can later claim it. Existing profiles are
     * returned untouched.
     *
     * Reservation can collide - two uids can end in the same six characters -
     * so this walks [FlyIdRules.defaultHandle] forward until one sticks.
     */
    suspend fun ensureProfile(uid: String): FlyProfile {
        val db = firestore ?: return defaultProfile(uid)
        return runCatching {
            db.userDocument(uid).get().await().toProfile(uid)?.let { return it }

            repeat(DEFAULT_HANDLE_ATTEMPTS) { attempt ->
                val candidate = FlyIdRules.defaultHandle(uid, attempt)
                val claimed = runCatching { db.reserve(uid, candidate, isChange = false) }
                when {
                    claimed.isSuccess -> return claimed.getOrThrow()
                    // Anything other than a collision will not resolve by
                    // trying a different id, so stop rather than hammer it.
                    claimed.exceptionOrNull() !is HandleTakenException -> return defaultProfile(uid)
                }
            }
            defaultProfile(uid)
        }.getOrElse { defaultProfile(uid) }
    }

    /**
     * Spends the account's single id change on [requested].
     *
     * Re-claiming the id the account already holds succeeds without consuming
     * the change: retyping your own id is a no-op, not a wasted rename.
     */
    suspend fun claimFlyId(uid: String, requested: String): ClaimResult {
        val handle = FlyIdRules.normalise(requested)
        FlyIdRules.validate(handle)?.let { return ClaimResult.Invalid(it) }

        val db = firestore
            ?: return ClaimResult.Failure("FlyDrop IDs need Firebase, which is not configured.")

        return try {
            ClaimResult.Success(db.reserveWithReconnect(uid, handle, isChange = true))
        } catch (_: HandleTakenException) {
            ClaimResult.Taken
        } catch (_: AlreadyChangedException) {
            ClaimResult.AlreadyChanged
        } catch (e: Exception) {
            ClaimResult.Failure(e.friendlyMessage())
        }
    }

    /**
     * Finds the account holding [requested], if any.
     *
     * The permanent reservation is read first, followed by its owner's public
     * profile. Reservations are deliberately never deleted, so the second read
     * is required to distinguish a current id from one the account retired.
     *
     * The reservation carries no display name - the app stores none - so a
     * result is identified by the FlyDrop ID itself and the avatar derived from
     * its owner's uid, the same way peers are drawn everywhere else.
     */
    suspend fun findByFlyId(requested: String): FlyIdSearchResult {
        val handle = FlyIdRules.normalise(requested)
        FlyIdRules.validate(handle)?.let { return FlyIdSearchResult.Invalid(it) }

        val db = firestore
            ?: return FlyIdSearchResult.Failure(
                "Searching needs Firebase, which is not configured.",
            )

        return runCatching {
            val reservation = db.handleDocument(handle).get().await()
            val uid = reservation.takeIf { it.exists() }?.getString(FIELD_UID)
                ?.takeIf { it.isNotBlank() }
                ?: return@runCatching null
            val profile = db.userDocument(uid).get().await().toProfile(uid)
            uid to profile
        }
            .fold(
                onSuccess = { ownerAndProfile ->
                    val uid = ownerAndProfile?.first
                    val profile = ownerAndProfile?.second
                    if (uid == null || profile == null || !isCurrentFlyId(handle, profile.handle)) {
                        FlyIdSearchResult.NotFound
                    } else {
                        FlyIdSearchResult.Found(
                            FlyUser(
                                id = uid,
                                name = profile.flyId,
                                flyId = profile.flyId,
                                avatarSeed = uid.hashCode().ushr(1),
                            ),
                        )
                    }
                },
                onFailure = { FlyIdSearchResult.Failure(it.friendlyMessage()) },
            )
    }

    /**
     * Reserves [handle] for [uid] and points the account's profile at it, or
     * throws. Both documents are written in one transaction, so an account can
     * never end up holding a reservation its profile does not name, and the
     * reads guarding the two rules happen inside it - which is what makes two
     * devices racing for one id resolve to a single winner.
     */
    private suspend fun FirebaseFirestore.reserve(
        uid: String,
        handle: String,
        isChange: Boolean,
    ): FlyProfile =
        runTransaction { transaction ->
            val userRef = userDocument(uid)
            val handleRef = handleDocument(handle)

            // Firestore requires every read to precede every write in a transaction.
            val userSnapshot = transaction.get(userRef)
            val handleSnapshot = transaction.get(handleRef)
            val current = userSnapshot.toProfile(uid)

            // Retyping the current id is a no-op. Handling it inside the
            // transaction avoids a separate network read and keeps the answer
            // consistent with the writes guarded below.
            if (current?.handle == handle) return@runTransaction current

            if (isChange && current?.handleChanged == true) {
                throw AlreadyChangedException()
            }
            if (handleSnapshot.exists()) {
                throw HandleTakenException()
            }

            transaction.set(
                handleRef,
                mapOf(FIELD_UID to uid, FIELD_CREATED_AT to FieldValue.serverTimestamp()),
            )
            transaction.set(
                userRef,
                mapOf(
                    FIELD_HANDLE to handle,
                    FIELD_HANDLE_CHANGED to isChange,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            FlyProfile(uid, handle, handleChanged = isChange)
        }.await()

    /**
     * Firestore reconnects itself normally. If it reports a transient network
     * failure, explicitly re-enable its network and retry the whole transaction
     * once. Retrying the transaction is safe because the server-side rules and
     * document reads keep the operation atomic and idempotent.
     */
    private suspend fun FirebaseFirestore.reserveWithReconnect(
        uid: String,
        handle: String,
        isChange: Boolean,
    ): FlyProfile {
        return try {
            reserve(uid, handle, isChange)
        } catch (error: FirebaseFirestoreException) {
            if (!error.code.isTransientConnectionFailure()) throw error
            runCatching { enableNetwork().await() }
            delay(RECONNECT_DELAY_MS)
            reserve(uid, handle, isChange)
        }
    }

    private fun FirebaseFirestore.userDocument(uid: String): DocumentReference =
        collection(USERS).document(uid)

    private fun FirebaseFirestore.handleDocument(handle: String): DocumentReference =
        collection(FLY_IDS).document(handle)

    private fun DocumentSnapshot.toProfile(uid: String): FlyProfile? {
        val handle = takeIf { it.exists() }
            ?.getString(FIELD_HANDLE)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return FlyProfile(uid, handle, handleChanged = getBoolean(FIELD_HANDLE_CHANGED) == true)
    }

    /** What the account is called before - or without - a stored profile. */
    private fun defaultProfile(uid: String) =
        FlyProfile(uid, FlyIdRules.defaultHandle(uid), handleChanged = false)

    private companion object {
        const val USERS = "users"
        const val FLY_IDS = "flyIds"
        const val FIELD_UID = "uid"
        const val FIELD_HANDLE = "handle"
        const val FIELD_HANDLE_CHANGED = "handleChanged"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val DEFAULT_HANDLE_ATTEMPTS = 8
        const val RECONNECT_DELAY_MS = 500L
    }
}

internal fun isCurrentFlyId(requestedHandle: String, profileHandle: String?): Boolean =
    requestedHandle == profileHandle

/**
 * Thrown from inside the claim transaction. Firestore surfaces whatever a
 * transaction body throws, which is how the outcome travels back out.
 */
private class HandleTakenException : Exception("That FlyDrop ID is taken.")

private class AlreadyChangedException : Exception("This FlyDrop ID has already been changed.")

private fun FirebaseFirestoreException.Code.isTransientConnectionFailure(): Boolean =
    this == FirebaseFirestoreException.Code.UNAVAILABLE ||
        this == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED

private fun Throwable.failureReason(): ProfileFailureReason =
    when ((this as? FirebaseFirestoreException)?.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> ProfileFailureReason.Connection

        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            ProfileFailureReason.Unauthenticated

        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            ProfileFailureReason.PermissionDenied

        FirebaseFirestoreException.Code.NOT_FOUND,
        FirebaseFirestoreException.Code.FAILED_PRECONDITION,
        -> ProfileFailureReason.ServiceNotReady

        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            ProfileFailureReason.ServiceBusy

        else -> ProfileFailureReason.Unknown
    }

private fun Throwable.friendlyMessage(): String = profileFailureMessage(failureReason())
