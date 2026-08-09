package com.appotato.shared.telemetry.implementation

import android.content.Context
import android.util.Log
import com.appotato.shared.telemetry.api.Telemetry
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TAG = "Telemetry"

public fun telemetryModule(): Module = module {
    single<Telemetry> { createTelemetry(androidContext()) }
}

private fun createTelemetry(context: Context): Telemetry =
    if (FirebaseApp.getApps(context).isEmpty()) {
        Log.w(TAG, "Firebase is not configured — google-services.json is missing. Telemetry is disabled.")
        NoOpTelemetry()
    } else {
        FirebaseTelemetry(
            analytics = FirebaseAnalytics.getInstance(context),
            crashlytics = FirebaseCrashlytics.getInstance()
        )
    }
