package com.flydrop.app.data.profile

/** User-facing categories for failures from the remote FlyDrop ID service. */
internal enum class ProfileFailureReason {
    Connection,
    Unauthenticated,
    PermissionDenied,
    ServiceNotReady,
    ServiceBusy,
    Unknown,
}

internal fun profileFailureMessage(reason: ProfileFailureReason): String =
    when (reason) {
        ProfileFailureReason.Connection ->
            "Could not reach FlyDrop. Check your internet connection, then tap Save to retry."

        ProfileFailureReason.Unauthenticated ->
            "Your sign-in session expired. Sign out, then sign in again."

        ProfileFailureReason.PermissionDenied ->
            "FlyDrop ID access was denied. Update the app or contact support."

        ProfileFailureReason.ServiceNotReady ->
            "The FlyDrop ID service is not ready. Please try again later."

        ProfileFailureReason.ServiceBusy ->
            "The FlyDrop ID service is busy. Try again in a moment."

        ProfileFailureReason.Unknown ->
            "Could not update your FlyDrop ID. Please try again."
    }
