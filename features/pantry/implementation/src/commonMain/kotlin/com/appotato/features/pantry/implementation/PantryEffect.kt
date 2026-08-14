package com.appotato.features.pantry.implementation

internal sealed interface PantryEffect {
    /**
     * Either the free tier ran out mid-add or the user asked to upgrade. The paywall is a separate
     * feature, so the host decides how to show it.
     */
    data object PaywallRequested : PantryEffect
}
