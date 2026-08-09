package com.appotato.shared.telemetry.fake

import com.appotato.shared.telemetry.api.Telemetry
import com.appotato.shared.telemetry.api.TelemetryEvent

public class TelemetryFake : Telemetry {

    private val recordedEvents = mutableListOf<TelemetryEvent>()
    private val recordedErrors = mutableListOf<Throwable>()
    private val recordedLogs = mutableListOf<String>()

    public val events: List<TelemetryEvent> get() = recordedEvents.toList()
    public val errors: List<Throwable> get() = recordedErrors.toList()
    public val logs: List<String> get() = recordedLogs.toList()

    public var userId: String? = null
        private set

    override fun track(event: TelemetryEvent) {
        recordedEvents += event
    }

    override fun setUserId(userId: String?) {
        this.userId = userId
    }

    override fun log(message: String) {
        recordedLogs += message
    }

    override fun recordError(error: Throwable) {
        recordedErrors += error
    }

    public fun clear() {
        recordedEvents.clear()
        recordedErrors.clear()
        recordedLogs.clear()
        userId = null
    }
}
