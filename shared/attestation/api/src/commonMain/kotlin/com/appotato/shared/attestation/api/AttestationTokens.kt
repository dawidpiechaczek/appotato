package com.appotato.shared.attestation.api

/**
 * Proof, to our own backend, that a request came from a genuine build of this app.
 *
 * The app has no user accounts, so there is nothing to authenticate a caller *as* — but the backend
 * holds a key that spends money, and an open endpoint is an open wallet. This is what stands in
 * for a login: an attestation the server can verify and a forged client cannot produce.
 *
 * Nothing here names a vendor, same as `Telemetry` and `RemoteConfig`. It is also deliberately not
 * about recipes: any future endpoint we proxy needs exactly this, and none of them should have to
 * depend on a recipe module to get it.
 */
public interface AttestationTokens {

    /**
     * Calls [onResult] with a token, or with `null` when one could not be obtained — the device
     * failed the integrity check, the app is unregistered, or there is no connection. A caller that
     * gets `null` should give up rather than send the request: the server will reject it anyway.
     *
     * A callback and not a `suspend` function because the iOS binding is written in Swift, and a
     * Kotlin `suspend` member cannot be overridden from Swift. Kotlin callers use the suspending
     * [token] extension instead.
     *
     * Implementations must never throw.
     */
    public fun token(onResult: (String?) -> Unit)
}
