package com.appotato.shared.attestation.implementation

import com.appotato.shared.attestation.api.AttestationTokens

/**
 * What a build with no Firebase configuration gets. Every call reports "no token", which is the
 * honest answer — and callers already have to handle it, because a real device can fail the
 * integrity check for reasons that have nothing to do with configuration.
 */
internal class NoOpAttestationTokens : AttestationTokens {
    override fun token(onResult: (String?) -> Unit) {
        onResult(null)
    }
}
