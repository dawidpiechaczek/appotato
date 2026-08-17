package com.appotato.shared.attestation.implementation

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appotato.shared.attestation.api.AttestationTokens
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TAG = "Attestation"

/**
 * `createdAtStart` because installing the App Check provider is app-startup work, not a dependency
 * to be resolved on first use. Left lazy, none of the setup below runs until something asks for an
 * `AttestationTokens` — so the provider is not registered, no token is fetched in the background,
 * and the debug provider never prints the secret you need to register.
 */
public fun attestationModule(): Module = module {
    single<AttestationTokens>(createdAtStart = true) { createAttestationTokens(androidContext()) }
}

/**
 * Mirrors how remote config degrades: without `google-services.json` there is no Firebase app to
 * attest against, so the binding still resolves and simply never produces a token. The alternative
 * is a crash on startup in a build that is otherwise perfectly runnable.
 */
private fun createAttestationTokens(context: Context): AttestationTokens =
    if (FirebaseApp.getApps(context).isEmpty()) {
        Log.w(TAG, "Firebase is not configured — google-services.json is missing. App Check is disabled.")
        NoOpAttestationTokens()
    } else {
        FirebaseAttestationTokens(
            appCheck = FirebaseAppCheck.getInstance().apply {
                // Play Integrity cannot attest a debuggable build, so debug uses the provider that
                // prints a token to logcat — register that token in the Firebase console once and
                // the emulator works. Shipping the debug provider in a release build would make
                // the whole mechanism decorative, hence the branch.
                installAppCheckProviderFactory(
                    if (context.isDebuggable()) {
                        DebugAppCheckProviderFactory.getInstance()
                    } else {
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    }
                )
                // Fetches a token in the background now and keeps it fresh, so the first request
                // that needs one is not also paying for a Play Integrity round trip — on a cold
                // device that attestation is seconds, and it would land on top of the model call
                // the user is already waiting for.
                //
                // It is also what makes the debug provider print its secret at startup: the
                // provider is created on the first token request, and without this nothing would
                // ask until a feature did.
                setTokenAutoRefreshEnabled(true)
            }
        )
    }

private fun Context.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
