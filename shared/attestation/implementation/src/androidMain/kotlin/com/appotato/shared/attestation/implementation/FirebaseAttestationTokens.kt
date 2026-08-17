package com.appotato.shared.attestation.implementation

import com.appotato.shared.attestation.api.AttestationTokens
import com.google.firebase.appcheck.FirebaseAppCheck

internal class FirebaseAttestationTokens(
    private val appCheck: FirebaseAppCheck
) : AttestationTokens {

    /**
     * `getAppCheckToken(false)` reuses the cached token until it is close to expiry, which is what
     * we want: a fresh attestation on every suggestion request would mean a Play Integrity round
     * trip on every suggestion request.
     *
     * The failure listener reports `null` rather than propagating, because the contract says this
     * never throws and every caller already has to handle "no token".
     */
    override fun token(onResult: (String?) -> Unit) {
        appCheck.getAppCheckToken(false)
            .addOnSuccessListener { result -> onResult(result.token) }
            .addOnFailureListener { onResult(null) }
    }
}
