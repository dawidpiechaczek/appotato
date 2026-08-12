package com.appotato.features.paywall.implementation

internal sealed interface PaywallEffect {
    /** The entitlement is now owned — bought here or restored. Leave the paywall. */
    data object Subscribed : PaywallEffect

    /** The user left without buying. */
    data object Dismissed : PaywallEffect
}
