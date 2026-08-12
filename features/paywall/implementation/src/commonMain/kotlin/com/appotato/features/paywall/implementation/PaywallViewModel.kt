package com.appotato.features.paywall.implementation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appotato.shared.billing.api.Billing
import com.appotato.shared.billing.api.BillingError
import com.appotato.shared.billing.api.Entitlement
import com.appotato.shared.billing.api.SubscriptionPeriod
import com.appotato.shared.billing.api.SubscriptionPlan
import com.appotato.shared.billing.api.SubscriptionStatus
import com.appotato.shared.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The screen sells exactly one thing; every success path is checked against this. */
private val SoldEntitlement = Entitlement.Pro

internal class PaywallViewModel(
    private val billing: Billing,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    private val _effects = Channel<PaywallEffect>(Channel.BUFFERED)
    val effects: Flow<PaywallEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: PaywallIntent) {
        when (intent) {
            PaywallIntent.ScreenShown, PaywallIntent.RetryClicked -> loadPlans()
            is PaywallIntent.PlanSelected -> _state.update { it.copy(selectedPlanId = intent.planId) }
            PaywallIntent.PurchaseClicked -> purchase()
            PaywallIntent.RestoreClicked -> restore()
            PaywallIntent.MessageDismissed -> _state.update { it.copy(message = null) }
            PaywallIntent.CloseClicked -> launchOnIo { _effects.send(PaywallEffect.Dismissed) }
        }
    }

    private fun loadPlans() {
        _state.update { it.copy(isLoading = true, message = null) }

        launchOnIo {
            billing.plans().fold(
                onSuccess = { plans -> onPlansLoaded(plans) },
                onFailure = { onPlansFailed() }
            )
        }
    }

    private fun onPlansLoaded(plans: List<SubscriptionPlan>) = _state.update { state ->
        state.copy(
            isLoading = false,
            plans = plans,
            // Keep what the user already picked — a retry must not move the selection under them.
            selectedPlanId = state.selectedPlanId ?: bestValuePlanId(plans)
        )
    }

    private fun onPlansFailed() = _state.update {
        it.copy(isLoading = false, message = PaywallMessage.PlansUnavailable)
    }

    private fun purchase() {
        val plan = _state.value.selectedPlan
        if (plan == null || _state.value.isWorking) return

        // Flipped here and not inside the coroutine: with the flag set only once the coroutine
        // runs, a double tap opens two store sheets.
        _state.update { it.copy(isWorking = true, message = null) }

        launchOnIo {
            val result = billing.purchase(plan)
            _state.update { it.copy(isWorking = false) }
            result.fold(
                onSuccess = { _effects.send(PaywallEffect.Subscribed) },
                onFailure = { error -> onPurchaseFailed(error) }
            )
        }
    }

    /** Cancelling is not a failure the user needs telling about — they just did it on purpose. */
    private fun onPurchaseFailed(error: Throwable) {
        if (error !is BillingError.Cancelled) {
            _state.update { it.copy(message = PaywallMessage.PurchaseFailed) }
        }
    }

    private fun restore() {
        if (_state.value.isWorking) return

        _state.update { it.copy(isWorking = true, message = null) }

        launchOnIo {
            val result = billing.restore()
            _state.update { it.copy(isWorking = false) }
            result.fold(
                onSuccess = { status -> onRestored(status) },
                onFailure = { onRestoreFailed() }
            )
        }
    }

    /** A restore that finds nothing succeeded — it just has nothing to hand back. */
    private suspend fun onRestored(status: SubscriptionStatus) {
        if (SoldEntitlement in status.entitlements) {
            _effects.send(PaywallEffect.Subscribed)
        } else {
            _state.update { it.copy(message = PaywallMessage.NothingToRestore) }
        }
    }

    private fun onRestoreFailed() = _state.update {
        it.copy(message = PaywallMessage.RestoreFailed)
    }

    private fun launchOnIo(block: suspend () -> Unit) = viewModelScope.launch(dispatchers.io) { block() }
}

/** Yearly is preselected — it is the cheapest per month, so it is the one to nudge towards. */
private fun bestValuePlanId(plans: List<SubscriptionPlan>): String? =
    (plans.firstOrNull { it.period == SubscriptionPeriod.Yearly } ?: plans.firstOrNull())?.id
