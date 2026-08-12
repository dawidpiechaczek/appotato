package com.appotato.shared.billing.fake

import com.appotato.shared.billing.api.Billing
import com.appotato.shared.billing.api.BillingError
import com.appotato.shared.billing.api.Entitlement
import com.appotato.shared.billing.api.SubscriptionPeriod
import com.appotato.shared.billing.api.SubscriptionPlan
import com.appotato.shared.billing.api.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [Billing] with two stock plans.
 *
 * A successful [purchase] flips [status] to `Active` the way a store SDK would, so a gate under
 * test reacts to the same signal it will react to in production. Set one of the `…Error` properties
 * to exercise a failure path — [BillingError.Cancelled] in particular, which is the one the UI has
 * to stay quiet about.
 */
public class BillingFake(
    initialStatus: SubscriptionStatus = SubscriptionStatus.None,
    public var plans: List<SubscriptionPlan> = DefaultPlans
) : Billing {

    private val _status = MutableStateFlow(initialStatus)
    override val status: StateFlow<SubscriptionStatus> = _status.asStateFlow()

    public var plansError: BillingError? = null
    public var purchaseError: BillingError? = null
    public var restoreError: BillingError? = null

    /** What [restore] reports on success — default is "the store had nothing for this user". */
    public var restoredStatus: SubscriptionStatus = SubscriptionStatus.None

    /** Every plan [purchase] was called with, failed attempts included, in call order. */
    public var purchaseAttempts: List<SubscriptionPlan> = emptyList()
        private set

    public var restoreCount: Int = 0
        private set

    /** Pushes a status change the way the store SDK would, without going through a purchase. */
    public fun emit(status: SubscriptionStatus) {
        _status.value = status
    }

    override suspend fun plans(): Result<List<SubscriptionPlan>> =
        plansError?.let { Result.failure(it) } ?: Result.success(plans)

    override suspend fun purchase(plan: SubscriptionPlan): Result<SubscriptionStatus> {
        purchaseAttempts = purchaseAttempts + plan
        val failure = purchaseError
        return if (failure != null) {
            Result.failure(failure)
        } else {
            Result.success(activeFor(plan).also { _status.value = it })
        }
    }

    override suspend fun restore(): Result<SubscriptionStatus> {
        restoreCount++
        val failure = restoreError
        return if (failure != null) {
            Result.failure(failure)
        } else {
            Result.success(restoredStatus.also { _status.value = it })
        }
    }

    private fun activeFor(plan: SubscriptionPlan) = SubscriptionStatus.Active(
        planId = plan.id,
        entitlements = setOf(plan.entitlement),
        expiresAtEpochMillis = ExpiresAtEpochMillis,
        willRenew = true,
        isTrial = plan.freeTrialDays > 0
    )

    public companion object {

        public val MonthlyPro: SubscriptionPlan = SubscriptionPlan(
            id = "pro_monthly",
            entitlement = Entitlement.Pro,
            period = SubscriptionPeriod.Monthly,
            formattedPrice = "12,99 zł"
        )

        public val YearlyPro: SubscriptionPlan = SubscriptionPlan(
            id = "pro_yearly",
            entitlement = Entitlement.Pro,
            period = SubscriptionPeriod.Yearly,
            formattedPrice = "99,00 zł",
            freeTrialDays = 7
        )

        public val DefaultPlans: List<SubscriptionPlan> = listOf(MonthlyPro, YearlyPro)

        /** Far enough out that no test has to care; tests assert on the shape, not on the clock. */
        private const val ExpiresAtEpochMillis = 4_102_444_800_000L
    }
}
