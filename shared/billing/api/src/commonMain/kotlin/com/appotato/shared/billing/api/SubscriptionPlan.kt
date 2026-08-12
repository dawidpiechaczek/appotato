package com.appotato.shared.billing.api

/**
 * One purchasable option on the paywall.
 *
 * [formattedPrice] is the string the store returned, already localised for the user's storefront
 * currency and locale. Never rebuild it from a number: the price of a plan differs per country,
 * and so does where the currency symbol goes.
 */
public data class SubscriptionPlan(
    public val id: String,
    public val entitlement: Entitlement,
    public val period: SubscriptionPeriod,
    public val formattedPrice: String,
    /** Length of the introductory free trial, or 0 when the plan has none. */
    public val freeTrialDays: Int = 0
)
