package com.appotato.shared.app.update.api

/** What the server says about the installed build. */
public sealed interface AppUpdateStatus {

    /** Installed version is at or above everything the server asks for. */
    public data object UpToDate : AppUpdateStatus

    /** Common shape of the two update cases, so the UI can read them without a second `when`. */
    public sealed interface Update : AppUpdateStatus {
        /** The version the server points at — the minimum for [Required], the newest for [Available]. */
        public val version: AppVersion

        /** Copy from the console, or null when none was published. */
        public val message: String?

        /** Store listing to open, or null when none was published. */
        public val storeUrl: String?
    }

    /** A newer build exists. The user may dismiss this and keep using the app. */
    public data class Available(
        override val version: AppVersion,
        override val message: String? = null,
        override val storeUrl: String? = null
    ) : Update

    /** The installed build is below the minimum supported version and must be blocked. */
    public data class Required(
        override val version: AppVersion,
        override val message: String? = null,
        override val storeUrl: String? = null
    ) : Update
}
