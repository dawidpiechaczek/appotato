package com.appotato.shared.telemetry.implementation

import com.appotato.shared.telemetry.api.Telemetry
import com.appotato.shared.telemetry.api.TelemetryEvent

/**
 * Fallback used when the platform SDK is not configured. Dropping events is the correct
 * behaviour there — refusing to start the app because analytics is misconfigured is not.
 */
internal class NoOpTelemetry : Telemetry {
    override fun track(event: TelemetryEvent): Unit = Unit
    override fun setUserId(userId: String?): Unit = Unit
    override fun log(message: String): Unit = Unit
    override fun recordError(error: Throwable): Unit = Unit
}
