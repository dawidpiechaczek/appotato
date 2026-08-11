package com.appotato.shared.app.update.api

public interface AppUpdateChecker {

    /** The version this build reports — useful for "you have x.y.z" copy and for logging. */
    public val installedVersion: AppVersion?

    /**
     * Fetches the current thresholds and compares them against [installedVersion].
     *
     * No `Result`: this cannot fail in a way the caller could act on. A failed fetch falls back to
     * the last activated values, and unreadable values mean [AppUpdateStatus.UpToDate] — blocking
     * the app because a config value was typed wrong is worse than missing one forced update.
     */
    public suspend fun check(): AppUpdateStatus
}
