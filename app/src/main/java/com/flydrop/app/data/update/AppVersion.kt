package com.flydrop.app.data.update

/**
 * A version number, comparable against another.
 *
 * Release tags and `versionName` are written by hand and do not always agree on
 * spelling - `v1.0.2` against `1.0.2`, or three segments against two - so both
 * sides are parsed into the same shape before anything is compared. An update
 * offer is only as trustworthy as this comparison, which is why it is plain
 * Kotlin with no Android in it and covered by tests.
 */
data class AppVersion(
    val numbers: List<Int>,
    /** The `-beta.1` in `1.2.0-beta.1`, or null for a final release. */
    val preRelease: String?,
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val length = maxOf(numbers.size, other.numbers.size)
        for (index in 0 until length) {
            // A missing segment is zero, so 1.2 and 1.2.0 are the same version.
            val mine = numbers.getOrElse(index) { 0 }
            val theirs = other.numbers.getOrElse(index) { 0 }
            if (mine != theirs) return mine.compareTo(theirs)
        }

        // Same numbers: a pre-release precedes the final release it leads to,
        // so 1.2.0-beta is older than 1.2.0 rather than newer than it.
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> comparePreRelease(preRelease, other.preRelease)
        }
    }

    override fun toString(): String =
        numbers.joinToString(".") + (preRelease?.let { "-$it" } ?: "")

    companion object {
        /**
         * Null when [raw] carries no version this can reason about. Callers
         * must treat that as "cannot tell" and say so, rather than falling back
         * to a guess - offering a downgrade, or silently hiding a real update,
         * are both worse than admitting the tag was not understood.
         */
        fun parse(raw: String): AppVersion? {
            val trimmed = raw.trim().removePrefix("v").removePrefix("V").trim()
            if (trimmed.isEmpty()) return null

            // A separator with nothing after it is a malformed tag. Reading
            // `1.0.2-` as `1.0.2` would be guessing at what was meant.
            if (trimmed.endsWith('-') || trimmed.endsWith('+')) return null

            val withoutBuildMetadata = trimmed.substringBefore('+')
            val preRelease = withoutBuildMetadata.substringAfter('-', "").ifEmpty { null }
            val numeric = withoutBuildMetadata.substringBefore('-')

            if (preRelease != null && !preRelease.isValidIdentifierList()) return null

            val buildMetadata = trimmed.substringAfter('+', "").ifEmpty { null }
            if ('+' in trimmed && (buildMetadata == null || !buildMetadata.isValidIdentifierList())) {
                return null
            }

            val numbers = numeric.split('.').map { segment ->
                segment.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null
            }
            return if (numbers.isEmpty()) null else AppVersion(numbers, preRelease)
        }

        /**
         * True when [candidate] is a release worth offering over [installed].
         * False whenever either side cannot be parsed: an unreadable tag is not
         * grounds for pushing an install at someone.
         */
        fun isNewer(candidate: String, installed: String): Boolean {
            val new = parse(candidate) ?: return false
            val current = parse(installed) ?: return false
            return new > current
        }
    }
}

/** Semantic-version precedence for dot-separated prerelease identifiers. */
private fun comparePreRelease(left: String, right: String): Int {
    val mine = left.split('.')
    val theirs = right.split('.')
    for (index in 0 until minOf(mine.size, theirs.size)) {
        val comparison = compareIdentifier(mine[index], theirs[index])
        if (comparison != 0) return comparison
    }
    return mine.size.compareTo(theirs.size)
}

private fun compareIdentifier(left: String, right: String): Int {
    val leftIsNumber = left.all { it in '0'..'9' }
    val rightIsNumber = right.all { it in '0'..'9' }
    return when {
        leftIsNumber && rightIsNumber -> compareNumericText(left, right)
        leftIsNumber -> -1
        rightIsNumber -> 1
        else -> left.compareTo(right)
    }
}

/** Compares arbitrary-length numeric identifiers without overflowing an Int. */
private fun compareNumericText(left: String, right: String): Int {
    val normalisedLeft = left.trimStart('0').ifEmpty { "0" }
    val normalisedRight = right.trimStart('0').ifEmpty { "0" }
    return normalisedLeft.length.compareTo(normalisedRight.length)
        .takeIf { it != 0 }
        ?: normalisedLeft.compareTo(normalisedRight)
}

private fun String.isValidIdentifierList(): Boolean =
    split('.').all { identifier ->
        identifier.isNotEmpty() && identifier.all { it.isLetterOrDigit() || it == '-' }
    }
