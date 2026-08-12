package com.appotato.features.paywall.implementation

internal sealed interface PaywallIntent {
    data object ScreenShown : PaywallIntent
    data object RetryClicked : PaywallIntent
    data class PlanSelected(val planId: String) : PaywallIntent
    data object PurchaseClicked : PaywallIntent
    data object RestoreClicked : PaywallIntent
    data object MessageDismissed : PaywallIntent
    data object CloseClicked : PaywallIntent
}
