package com.appotato.shared.remote.config.api

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Suspending form of [RemoteConfig.refresh]. Returns `true` when new values were fetched and
 * activated, `false` when the fetch failed — in which case the last activated values are still
 * what the getters return.
 *
 * Uses `suspendCoroutine` from the standard library rather than `suspendCancellableCoroutine`, so
 * this module needs no coroutines dependency: adding one would also have to be exported into the
 * iOS framework alongside the contract itself.
 */
public suspend fun RemoteConfig.refresh(): Boolean = suspendCoroutine { continuation ->
    refresh { succeeded -> continuation.resume(succeeded) }
}
