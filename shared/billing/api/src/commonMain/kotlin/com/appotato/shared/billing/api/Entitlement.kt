package com.appotato.shared.billing.api

/**
 * What a paid subscription unlocks.
 *
 * Gate features on these and never on a plan or a store product id: monthly and yearly grant the
 * same thing, and every repricing or store-side rename would otherwise reach into call sites.
 */
public enum class Entitlement {
    /** Everything behind the paywall. */
    Pro
}
