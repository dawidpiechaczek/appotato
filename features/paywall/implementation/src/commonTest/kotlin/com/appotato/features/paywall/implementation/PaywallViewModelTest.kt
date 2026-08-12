package com.appotato.features.paywall.implementation

import com.appotato.shared.billing.api.BillingError
import com.appotato.shared.billing.api.Entitlement
import com.appotato.shared.billing.api.SubscriptionStatus
import com.appotato.shared.billing.fake.BillingFake
import com.appotato.shared.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        main = dispatcher,
        default = dispatcher,
        io = dispatcher,
        unconfined = dispatcher
    )
    private val billing = BillingFake()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PaywallViewModel(billing, dispatchers)

    @Test
    fun `Given plans in the store When the screen is shown Then they are offered`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        assertEquals(BillingFake.DefaultPlans, viewModel.state.value.plans)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Given a yearly plan is offered When the screen is shown Then it is preselected`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        assertEquals(BillingFake.YearlyPro.id, viewModel.state.value.selectedPlanId)
    }

    @Test
    fun `Given only a monthly plan When the screen is shown Then it is preselected`() = runTest(dispatcher) {
        billing.plans = listOf(BillingFake.MonthlyPro)
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        assertEquals(BillingFake.MonthlyPro.id, viewModel.state.value.selectedPlanId)
    }

    @Test
    fun `Given no plans at all When the screen is shown Then nothing is selected`() = runTest(dispatcher) {
        billing.plans = emptyList()
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedPlanId)
        assertFalse(viewModel.state.value.canPurchase)
    }

    @Test
    fun `Given the store fails When the screen is shown Then the plans are reported unavailable`() =
        runTest(dispatcher) {
            billing.plansError = BillingError.Unavailable()
            val viewModel = viewModel()

            viewModel.onIntent(PaywallIntent.ScreenShown)
            advanceUntilIdle()

            assertEquals(PaywallMessage.PlansUnavailable, viewModel.state.value.message)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `Given the plans failed When retry is clicked Then they are fetched again`() = runTest(dispatcher) {
        billing.plansError = BillingError.Unavailable()
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        billing.plansError = null
        viewModel.onIntent(PaywallIntent.RetryClicked)
        advanceUntilIdle()

        assertEquals(BillingFake.DefaultPlans, viewModel.state.value.plans)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `Given the user picked a plan When the plans are reloaded Then the choice survives`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        viewModel.onIntent(PaywallIntent.PlanSelected(BillingFake.MonthlyPro.id))
        viewModel.onIntent(PaywallIntent.RetryClicked)
        advanceUntilIdle()

        assertEquals(BillingFake.MonthlyPro.id, viewModel.state.value.selectedPlanId)
        assertEquals(BillingFake.MonthlyPro, viewModel.state.value.selectedPlan)
    }

    @Test
    fun `Given a selected plan When purchase succeeds Then the subscribed effect is emitted`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        viewModel.onIntent(PaywallIntent.PurchaseClicked)
        advanceUntilIdle()

        assertEquals(PaywallEffect.Subscribed, viewModel.effects.first())
        assertEquals(listOf(BillingFake.YearlyPro), billing.purchaseAttempts)
        assertFalse(viewModel.state.value.isWorking)
    }

    @Test
    fun `Given the user cancels the store sheet When purchase returns Then no message is shown`() =
        runTest(dispatcher) {
            billing.purchaseError = BillingError.Cancelled()
            val viewModel = viewModel()
            viewModel.onIntent(PaywallIntent.ScreenShown)
            advanceUntilIdle()

            viewModel.onIntent(PaywallIntent.PurchaseClicked)
            advanceUntilIdle()

            assertNull(viewModel.state.value.message)
            assertFalse(viewModel.state.value.isWorking)
        }

    @Test
    fun `Given the store refuses When purchase returns Then the failure is shown`() = runTest(dispatcher) {
        billing.purchaseError = BillingError.NotAllowed()
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        viewModel.onIntent(PaywallIntent.PurchaseClicked)
        advanceUntilIdle()

        assertEquals(PaywallMessage.PurchaseFailed, viewModel.state.value.message)
    }

    @Test
    fun `Given no plan is selected When purchase is clicked Then the store is not called`() = runTest(dispatcher) {
        billing.plans = emptyList()
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        viewModel.onIntent(PaywallIntent.PurchaseClicked)
        advanceUntilIdle()

        assertTrue(billing.purchaseAttempts.isEmpty())
    }

    @Test
    fun `Given purchase is tapped twice When the first is still running Then only one sheet opens`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(PaywallIntent.ScreenShown)
            advanceUntilIdle()

            viewModel.onIntent(PaywallIntent.PurchaseClicked)
            viewModel.onIntent(PaywallIntent.PurchaseClicked)
            advanceUntilIdle()

            assertEquals(1, billing.purchaseAttempts.size)
        }

    @Test
    fun `Given a purchase is running When the state is read Then the call to action is disabled`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(PaywallIntent.ScreenShown)
            advanceUntilIdle()

            viewModel.onIntent(PaywallIntent.PurchaseClicked)

            assertTrue(viewModel.state.value.isWorking)
            assertFalse(viewModel.state.value.canPurchase)
        }

    @Test
    fun `Given the store account owns the subscription When restore runs Then the subscribed effect is emitted`() =
        runTest(dispatcher) {
            billing.restoredStatus = SubscriptionStatus.Active(
                planId = BillingFake.YearlyPro.id,
                entitlements = setOf(Entitlement.Pro),
                expiresAtEpochMillis = 1L,
                willRenew = true
            )
            val viewModel = viewModel()

            viewModel.onIntent(PaywallIntent.RestoreClicked)
            advanceUntilIdle()

            assertEquals(PaywallEffect.Subscribed, viewModel.effects.first())
            assertEquals(1, billing.restoreCount)
        }

    @Test
    fun `Given the store account owns nothing When restore runs Then it says so`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.RestoreClicked)
        advanceUntilIdle()

        assertEquals(PaywallMessage.NothingToRestore, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isWorking)
    }

    @Test
    fun `Given the store is unreachable When restore runs Then the failure is shown`() = runTest(dispatcher) {
        billing.restoreError = BillingError.Unavailable()
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.RestoreClicked)
        advanceUntilIdle()

        assertEquals(PaywallMessage.RestoreFailed, viewModel.state.value.message)
    }

    @Test
    fun `Given a restore is running When restore is clicked again Then the store is called once`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(PaywallIntent.RestoreClicked)
            viewModel.onIntent(PaywallIntent.RestoreClicked)
            advanceUntilIdle()

            assertEquals(1, billing.restoreCount)
        }

    @Test
    fun `Given a message is shown When it is dismissed Then it is cleared`() = runTest(dispatcher) {
        billing.plansError = BillingError.Unavailable()
        val viewModel = viewModel()
        viewModel.onIntent(PaywallIntent.ScreenShown)
        advanceUntilIdle()

        viewModel.onIntent(PaywallIntent.MessageDismissed)

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `Given the paywall is open When close is clicked Then the dismissed effect is emitted`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(PaywallIntent.CloseClicked)
        advanceUntilIdle()

        assertEquals(PaywallEffect.Dismissed, viewModel.effects.first())
        assertTrue(billing.purchaseAttempts.isEmpty())
    }
}
