package com.flydrop.app.data

import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.RadarDevice

/** Preview-only people used by Compose tooling; production screens never show them. */
object MockData {

    val guestUser = FlyUser(
        id = "guest",
        name = "Guest",
        flyId = "Sign in for a FlyDrop ID",
        avatarSeed = 0,
    )

    val currentUser = FlyUser(
        id = "me",
        name = "Lucas Sutopo",
        flyId = "fly#23941",
        avatarSeed = 1,
    )

    val ashley = FlyUser("ashley", "Ashley Nelson", "fly#112456", avatarSeed = 2, isFriend = true)
    val david = FlyUser("david", "David", "fly#884201", avatarSeed = 3, isFriend = true)
    val greyhold = FlyUser("greyhold", "Greyhold", "fly#553019", avatarSeed = 4, isFriend = true)
    val dennis = FlyUser("dennis", "Dennis", "fly#220874", avatarSeed = 5, isFriend = true)
    val gofar = FlyUser("gofar", "Gofar Badman", "fly#666666", avatarSeed = 6)
    val rudi = FlyUser("rudi", "Rudi Hartono", "fly#901233", avatarSeed = 7)

    /** Angles/radii chosen to reproduce the scatter in the reference radar. */
    val radarDevices = listOf(
        RadarDevice(david, angleDegrees = -22f, radiusFraction = 0.62f, avatarSize = 50),
        RadarDevice(ashley, angleDegrees = 74f, radiusFraction = 0.70f, avatarSize = 62),
        RadarDevice(rudi, angleDegrees = 152f, radiusFraction = 0.60f, avatarSize = 48),
        RadarDevice(greyhold, angleDegrees = 236f, radiusFraction = 0.66f, avatarSize = 52),
    )

    val nearbyFriends = listOf(ashley, gofar, rudi, david)
}
