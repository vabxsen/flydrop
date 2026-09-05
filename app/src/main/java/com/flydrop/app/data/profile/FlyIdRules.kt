package com.flydrop.app.data.profile

/**
 * Shape and spelling of a FlyDrop ID.
 *
 * The stored handle is always the lowercase form, because uniqueness is keyed
 * on it: `flyIds/{handle}` is the reservation document, so two spellings that
 * differ only in case must not be able to claim separate reservations.
 * Everything the user types is put through [normalise] before it is validated
 * or written, which is what makes "Lucas", "lucas" and "fly#Lucas" the same id.
 */
object FlyIdRules {

    /** Shown in front of the handle everywhere an id is displayed. */
    const val PREFIX = "fly#"

    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20

    /**
     * Lowercase alphanumerics, plus `.` and `_` as interior separators. Leading
     * and trailing separators are excluded by the anchors so an id can never
     * read as truncated, and [DOUBLE_SEPARATOR] rejects runs of them so two ids
     * cannot look near-identical at a glance.
     */
    private val SHAPE = Regex("^[a-z0-9](?:[a-z0-9._]*[a-z0-9])?$")
    private val DOUBLE_SEPARATOR = Regex("[._]{2}")

    /** Held back so nobody can pose as the app or its operators. Keep in sync with firestore.rules. */
    private val RESERVED = setOf(
        "flydrop", "fly", "admin", "administrator", "support", "help", "root",
        "system", "official", "team", "staff", "moderator", "security", "me",
    )

    /**
     * Strips the display prefix and folds case, so a user can paste back the id
     * exactly as the app shows it. Whitespace goes too: it is never valid, and
     * a stray space from a paste should not read as an error.
     */
    fun normalise(input: String): String {
        val trimmed = input.trim()
        val withoutPrefix = when {
            trimmed.length >= PREFIX.length &&
                trimmed.take(PREFIX.length).equals(PREFIX, ignoreCase = true) ->
                trimmed.drop(PREFIX.length)

            trimmed.startsWith('#') -> trimmed.drop(1)

            else -> trimmed
        }
        return withoutPrefix.trim().lowercase()
    }

    /** Null when [handle] — already [normalise]d — may be claimed. */
    fun validate(handle: String): FlyIdError? = when {
        handle.length < MIN_LENGTH -> FlyIdError.TooShort
        handle.length > MAX_LENGTH -> FlyIdError.TooLong
        !SHAPE.matches(handle) -> FlyIdError.BadShape
        DOUBLE_SEPARATOR.containsMatchIn(handle) -> FlyIdError.BadShape
        handle in RESERVED -> FlyIdError.Reserved
        else -> null
    }

    fun display(handle: String): String = PREFIX + handle

    /**
     * The id an account starts with, derived from its Firebase uid so it is
     * stable across installs and already close to unique. Non-alphanumerics are
     * dropped and the result is padded, so the derived handle always satisfies
     * [validate] no matter what the uid happens to contain.
     *
     * [attempt] disambiguates: the caller walks it upwards when a reservation
     * collides with an account that already holds that handle.
     */
    fun defaultHandle(uid: String, attempt: Int = 0): String {
        val base = uid.lowercase().filter { it.isLetterOrDigit() }
            .takeLast(6)
            .padStart(MIN_LENGTH, '0')
        return if (attempt == 0) base else base + attempt.toString(36)
    }
}

enum class FlyIdError(val message: String) {
    TooShort("Use at least ${FlyIdRules.MIN_LENGTH} characters."),
    TooLong("Use at most ${FlyIdRules.MAX_LENGTH} characters."),
    BadShape("Letters and numbers only, with . or _ in the middle."),
    Reserved("That id is reserved."),
}
