package com.flydrop.app.data.model

import androidx.compose.runtime.Immutable

/**
 * A FlyDrop user. [avatarSeed] deterministically drives the generated vector
 * avatar, so the same person always renders identically without any image asset.
 */
@Immutable
data class FlyUser(
    val id: String,
    val name: String,
    val flyId: String,
    val avatarSeed: Int,
    val isFriend: Boolean = false,
    /** Set for the signed-in account; null for mock peers. */
    val email: String? = null,
    /**
     * Google profile picture URL for the signed-in account. Not rendered today
     * (the app draws generated avatars and pulls no images), but carried so a
     * future image loader has it.
     */
    val photoUrl: String? = null,
)

enum class FileKind { Image, Archive, Pdf, Document }

@Immutable
data class TransferFile(
    val id: String,
    val name: String,
    val kind: FileKind,
    val totalBytes: Long,
    /** Null when the file is queued rather than in flight. */
    val transferredBytes: Long? = null,
) {
    val progress: Float?
        get() = transferredBytes?.let {
            if (totalBytes <= 0L) 0f else (it.toFloat() / totalBytes).coerceIn(0f, 1f)
        }
}

enum class TransferDirection { Incoming, Outgoing }

@Immutable
data class ActivityEntry(
    val id: String,
    val peer: FlyUser,
    val direction: TransferDirection,
    val fileCount: Int,
    val relativeTime: String,
)

/** Where a discovered device sits on the radar: [angleDegrees] clockwise from 12 o'clock. */
@Immutable
data class RadarDevice(
    val user: FlyUser,
    val angleDegrees: Float,
    /** 0f at the centre, 1f at the outer ring. */
    val radiusFraction: Float,
    val avatarSize: Int,
)

@Immutable
data class TransferState(
    val peer: FlyUser,
    val direction: TransferDirection,
    val progress: Float,
    val itemsLabel: String,
    val totalSizeLabel: String,
    val speedLabel: String,
    val files: List<TransferFile>,
)
