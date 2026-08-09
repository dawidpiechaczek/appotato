package com.appotato.shared.telemetry.implementation

import android.os.Bundle
import com.appotato.shared.telemetry.api.Telemetry
import com.appotato.shared.telemetry.api.TelemetryEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseTelemetry(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) : Telemetry {

    override fun track(event: TelemetryEvent) {
        val parameters = Bundle().apply {
            event.parameters.forEach { (key, value) -> putString(key, value) }
        }
        analytics.logEvent(event.name, parameters)
    }

    override fun setUserId(userId: String?) {
        analytics.setUserId(userId)
        // Crashlytics has no "unset": the empty string is how the identity is detached.
        crashlytics.setUserId(userId.orEmpty())
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordError(error: Throwable) {
        crashlytics.recordException(error)
    }
}
