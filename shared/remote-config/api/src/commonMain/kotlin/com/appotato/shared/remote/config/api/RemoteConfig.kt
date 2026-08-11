package com.appotato.shared.remote.config.api

/**
 * Server-controlled values read at runtime — feature flags, thresholds, the minimum app version.
 *
 * Nothing here names a vendor on purpose, same as `Telemetry`: replacing Firebase Remote Config
 * has to be a change inside :shared:remote-config:implementation and one line in the DI graph,
 * never a change in the call sites.
 *
 * Keys are plain strings and each consumer owns its own — a shared enum here would force every
 * feature to edit shared code to add a flag.
 *
 * Implementations must never throw. When nothing has been fetched yet, or the key is unknown, the
 * getters return the zero value of their type (`""`, `false`, `0`); callers decide what that means.
 */
public interface RemoteConfig {

    /**
     * Fetches the latest values and activates them, calling [onResult] with `true` when the fetch
     * succeeded. Previously activated values stay readable when it fails.
     *
     * A callback and not a `suspend` function because the iOS binding is written in Swift, and a
     * Kotlin `suspend` member cannot be overridden from Swift. Kotlin callers use the suspending
     * `RemoteConfig.refresh()` extension instead.
     */
    public fun refresh(onResult: (Boolean) -> Unit)

    public fun getString(key: String): String

    public fun getBoolean(key: String): Boolean

    public fun getLong(key: String): Long
}
