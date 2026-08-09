package com.appotato.shared.telemetry.api

/**
 * Sink for analytics events, breadcrumbs and non-fatal errors.
 *
 * Nothing here names a vendor on purpose: swapping Firebase for something else has to be a
 * change inside :shared:telemetry:implementation and one line in the DI graph, never a change
 * in the call sites.
 *
 * Implementations must never throw — telemetry failing is not a reason for the app to fail.
 */
public interface Telemetry {

    public fun track(event: TelemetryEvent)

    /** Pass null on sign-out so the identity is detached from subsequent events. */
    public fun setUserId(userId: String?)

    /** Breadcrumb attached to the next crash report. Not an analytics event. */
    public fun log(message: String)

    /** Records a handled error. Fatal crashes are captured by the platform SDK on its own. */
    public fun recordError(error: Throwable)
}
