package com.appotato.features.paywall.implementation

import androidx.compose.runtime.Immutable
import com.appotato.shared.billing.api.SubscriptionPlan

@Immutable
internal data class PaywallState(
    val isLoading: Boolean = true,
    val plans: List<SubscriptionPlan> = emptyList(),
    val selectedPlanId: String? = null,
    /** Covers both purchase and restore: either one owns the store sheet, so both block the CTA. */
    val isWorking: Boolean = false,
    val message: PaywallMessage? = null
) {
    val selectedPlan: SubscriptionPlan? = plans.firstOrNull { it.id == selectedPlanId }

    val canPurchase: Boolean = selectedPlan != null && !isWorking
}
