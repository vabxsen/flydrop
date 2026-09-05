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
    /**
     * Set for phone contacts that have a number, so an invite can be addressed
     * to them. Null for FlyDrop peers and for contacts stored without one.
     */
    val phoneNumber: String? = null,
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
