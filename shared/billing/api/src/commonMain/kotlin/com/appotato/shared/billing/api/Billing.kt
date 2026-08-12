package com.appotato.shared.billing.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Subscriptions, from the app's point of view.
 *
 * Nothing here names a vendor, same as `Telemetry` and `RemoteConfig`: which store or which
 * billing SDK sits behind this has to be a change inside :shared:billing:implementation and one
 * line in the DI graph. Store product ids stay inside the implementation too — [SubscriptionPlan]
 * ids are what the implementation chooses to expose, not what App Store Connect calls them.
 *
 * Implementations must treat [status] as the single source of truth and keep it current on their
 * own, including purchases made on another device or outside the app.
 */
public interface Billing {

    /**
     * What the user owns right now, updated by the implementation as the store reports changes.
     *
     * A [StateFlow] and not a plain flow so a gate can also ask synchronously — the very first
     * frame after a cold start has to render something, and blocking it on a store round trip
     * makes a paid app look like a free one for a second.
     */
    public val status: StateFlow<SubscriptionStatus>

    /**
     * The plans to offer, in no particular order. Prices come from the store, so this is a network
     * call and can fail with [BillingError.Unavailable].
     */
    public suspend fun plans(): Result<List<SubscriptionPlan>>

    /**
     * Runs the store's purchase flow for [plan] and returns the resulting status.
     *
     * Fails with [BillingError.Cancelled] when the user backs out — check for it before showing
     * anything.
     */
    public suspend fun purchase(plan: SubscriptionPlan): Result<SubscriptionStatus>

    /**
     * Re-reads what the store account already owns, for a user reinstalling or moving devices.
     *
     * Succeeding with [SubscriptionStatus.None] is a normal outcome — it means there was nothing
     * to restore — and is not the same as failing.
     */
    public suspend fun restore(): Result<SubscriptionStatus>
}
