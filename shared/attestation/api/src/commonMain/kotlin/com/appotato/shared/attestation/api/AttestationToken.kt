package com.appotato.shared.attestation.api

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Suspending form of [AttestationTokens.token]. Returns `null` when no token could be obtained.
 *
 * Uses `suspendCoroutine` from the standard library rather than `suspendCancellableCoroutine`, so
 * this module needs no coroutines dependency: adding one would also have to be exported into the
 * iOS framework alongside the contract itself.
 */
public suspend fun AttestationTokens.token(): String? = suspendCoroutine { continuation ->
    token { value -> continuation.resume(value) }
}
