package com.appotato.features.paywall.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appotato.shared.billing.api.SubscriptionPeriod
import com.appotato.shared.billing.api.SubscriptionPlan
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.HeaderText
import com.appotato.shared.ui.components.Loader
import com.appotato.shared.ui.components.OutlinedButton
import com.appotato.shared.ui.components.TextButton
import org.koin.compose.viewmodel.koinViewModel

private val ScreenPadding = 24.dp
private val SectionSpacing = 24.dp
private val ItemSpacing = 8.dp

/**
 * Entry point of the feature — the one declaration in this module that is not `internal`, the same
 * way `remoteConfigModule()` is the only public thing in its implementation module.
 *
 * [onSubscribed] and [onDismissed] are separate on purpose: the caller almost always closes the
 * paywall either way, but only one of the two should also refresh the screen behind it.
 */
@Composable
public fun PaywallRoute(
    onSubscribed: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PaywallViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PaywallEffect.Subscribed -> onSubscribed()
                PaywallEffect.Dismissed -> onDismissed()
            }
        }
    }

    PaywallScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun PaywallScreen(
    state: PaywallState,
    onIntent: (PaywallIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(PaywallIntent.ScreenShown)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Pitch()
        Spacer(modifier = Modifier.height(SectionSpacing))
        Plans(state = state, onIntent = onIntent)
        Spacer(modifier = Modifier.weight(weight = 1f))
        state.message?.let { message ->
            Message(message = message, onDismiss = { onIntent(PaywallIntent.MessageDismissed) })
        }
        Actions(state = state, onIntent = onIntent)
    }
}

@Composable
private fun Pitch() {
    HeaderText(text = "Appotato Pro", textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(ItemSpacing))
    BodyText(
        text = "Unlimited items, earlier reminders and barcode scanning.",
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ColumnScope.Plans(state: PaywallState, onIntent: (PaywallIntent) -> Unit) {
    when {
        state.isLoading -> Loader()

        state.plans.isEmpty() -> OutlinedButton(onClick = { onIntent(PaywallIntent.RetryClicked) }) {
            BodyText(text = "Try again")
        }

        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            state.plans.forEach { plan ->
                Plan(
                    plan = plan,
                    isSelected = plan.id == state.selectedPlanId,
                    onClick = { onIntent(PaywallIntent.PlanSelected(plan.id)) },
                )
            }
        }
    }
}

/**
 * Selection is carried by which wrapper is used rather than by a checkmark: the design system has
 * an elevated and an outlined button and nothing in between, so this stays inside it.
 */
@Composable
private fun Plan(plan: SubscriptionPlan, isSelected: Boolean, onClick: () -> Unit) {
    val label = "${plan.formattedPrice} / ${plan.period.label()}"
    if (isSelected) {
        ElevatedButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
            BodyText(text = label)
        }
    } else {
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
            BodyText(text = label)
        }
    }
}

@Composable
private fun Message(message: PaywallMessage, onDismiss: () -> Unit) {
    CommentText(
        text = message.text(),
        color = AppotatoTheme.colors.warning,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = onDismiss) {
        CommentText(text = "Dismiss")
    }
}

@Composable
private fun Actions(state: PaywallState, onIntent: (PaywallIntent) -> Unit) {
    ElevatedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canPurchase,
        onClick = { onIntent(PaywallIntent.PurchaseClicked) },
    ) {
        BodyText(text = state.selectedPlan.callToAction())
    }
    TextButton(
        enabled = !state.isWorking,
        onClick = { onIntent(PaywallIntent.RestoreClicked) },
    ) {
        CommentText(text = "Restore purchases")
    }
    TextButton(onClick = { onIntent(PaywallIntent.CloseClicked) }) {
        CommentText(text = "Not now")
    }
}

private fun SubscriptionPlan?.callToAction(): String = when {
    this == null -> "Choose a plan"
    freeTrialDays > 0 -> "Start $freeTrialDays days free"
    else -> "Subscribe for $formattedPrice"
}

private fun SubscriptionPeriod.label(): String = when (this) {
    SubscriptionPeriod.Monthly -> "month"
    SubscriptionPeriod.Yearly -> "year"
}

private fun PaywallMessage.text(): String = when (this) {
    PaywallMessage.PlansUnavailable -> "Could not load the plans. Check your connection."
    PaywallMessage.PurchaseFailed -> "The purchase did not go through."
    PaywallMessage.NothingToRestore -> "No subscription found on this store account."
    PaywallMessage.RestoreFailed -> "Could not reach the store. Try again."
}
