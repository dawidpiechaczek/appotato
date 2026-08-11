package com.appotato.shared.app.update.api

/**
 * A `major.minor.patch` version, compared numerically.
 *
 * String comparison is not an option here: `"1.10.0" < "1.9.0"` lexicographically, which would lock
 * out exactly the users who already updated.
 */
public data class AppVersion(
    public val major: Int,
    public val minor: Int = 0,
    public val patch: Int = 0
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this,
        other,
        AppVersion::major,
        AppVersion::minor,
        AppVersion::patch
    )

    override fun toString(): String = "$major.$minor.$patch"

    public companion object {

        /**
         * Parses `1`, `1.2`, `1.2.3` and `1.2.3-beta.1` — anything after the first `-` is dropped.
         * Returns null for everything else, a fourth segment included: a malformed value from the
         * console must not be able to decide that the installed app is out of date.
         */
        public fun parse(raw: String): AppVersion? {
            val segments = raw.trim().substringBefore('-').split('.')
            // Anything non-numeric drops out here, negatives included: the '-' is already gone.
            val numbers = segments.mapNotNull(String::toIntOrNull)
            if (numbers.size != segments.size || numbers.size > MAX_SEGMENTS) return null

            return AppVersion(
                major = numbers[0],
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 }
            )
        }

        private const val MAX_SEGMENTS = 3
    }
}
