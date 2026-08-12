package com.appotato.shared.billing.implementation

import com.appotato.shared.billing.api.Billing
import com.appotato.shared.billing.api.BillingError
import com.appotato.shared.billing.api.SubscriptionPlan
import com.appotato.shared.billing.api.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Everyone is on the free tier and nothing can be bought.
 *
 * Stands in until a store-backed implementation lands, and stays afterwards as the fallback for
 * builds where the billing SDK is not configured — the same role `NoOpRemoteConfig` plays in
 * :shared:remote-config:implementation. Failing every purchase with [BillingError.Unavailable] is
 * deliberate: a silent success here would hand out entitlements for free.
 */
internal class NoOpBilling : Billing {

    override val status: StateFlow<SubscriptionStatus> = MutableStateFlow(SubscriptionStatus.None)

    override suspend fun plans(): Result<List<SubscriptionPlan>> = Result.success(emptyList())

    override suspend fun purchase(plan: SubscriptionPlan): Result<SubscriptionStatus> =
        Result.failure(BillingError.Unavailable())

    override suspend fun restore(): Result<SubscriptionStatus> =
        Result.failure(BillingError.Unavailable())
}
