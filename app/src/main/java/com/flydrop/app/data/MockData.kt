package com.flydrop.app.data

import com.flydrop.app.data.model.ActivityEntry
import com.flydrop.app.data.model.FileKind
import com.flydrop.app.data.model.FlyUser
import com.flydrop.app.data.model.RadarDevice
import com.flydrop.app.data.model.TransferDirection
import com.flydrop.app.data.model.TransferFile
import com.flydrop.app.data.model.TransferState

/**
 * Placeholder state standing in for real discovery/transfer, kept out of the
 * composables so screens can be pointed at a real data source later.
 */
object MockData {

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

    val favouriteFriends = listOf(ashley, david, greyhold, dennis)

    val activities = listOf(
        ActivityEntry("a1", ashley, TransferDirection.Incoming, fileCount = 12, relativeTime = "2 min ago"),
        ActivityEntry("a2", greyhold, TransferDirection.Outgoing, fileCount = 3, relativeTime = "1 hour ago"),
        ActivityEntry("a3", david, TransferDirection.Incoming, fileCount = 8, relativeTime = "Yesterday"),
    )

    /** Angles/radii chosen to reproduce the scatter in the reference radar. */
    val radarDevices = listOf(
        RadarDevice(david, angleDegrees = -22f, radiusFraction = 0.62f, avatarSize = 50),
        RadarDevice(ashley, angleDegrees = 74f, radiusFraction = 0.70f, avatarSize = 62),
        RadarDevice(rudi, angleDegrees = 152f, radiusFraction = 0.60f, avatarSize = 48),
        RadarDevice(greyhold, angleDegrees = 236f, radiusFraction = 0.66f, avatarSize = 52),
    )

    val nearbyFriends = listOf(ashley, gofar, rudi, david)

    val transferFiles = listOf(
        TransferFile("f1", "IMG121235.jpg", FileKind.Image, totalBytes = 3_355_443, transferredBytes = 2_936_012),
        TransferFile("f2", "IMG121234.jpg", FileKind.Image, totalBytes = 3_145_728, transferredBytes = 1_572_864),
        TransferFile("f3", "Pokoke Nganu.zip", FileKind.Archive, totalBytes = 254_803_968),
        TransferFile("f4", "Peceland Project Brief.pdf", FileKind.Pdf, totalBytes = 13_002_342),
        TransferFile("f5", "Meeting Notes.docx", FileKind.Document, totalBytes = 486_539),
    )

    val transfer = TransferState(
        peer = ashley,
        direction = TransferDirection.Incoming,
        progress = 0.70f,
        itemsLabel = "5 to 12",
        totalSizeLabel = "812 MB",
        speedLabel = "21.3 MB/s",
        files = transferFiles,
    )
}
