package com.appotato.shared.billing.api

/** Billing cycle of a [SubscriptionPlan] — what the paywall renders as "/month" or "/year". */
public enum class SubscriptionPeriod {
    Monthly,
    Yearly
}
