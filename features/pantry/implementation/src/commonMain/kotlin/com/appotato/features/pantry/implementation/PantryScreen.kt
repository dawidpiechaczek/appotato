package com.appotato.features.pantry.implementation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.ElevatedButton
import com.appotato.shared.ui.components.HeaderText
import com.appotato.shared.ui.components.Loader
import com.appotato.shared.ui.components.OutlinedTextField
import com.appotato.shared.ui.components.TextButton
import org.koin.compose.viewmodel.koinViewModel

private val ScreenPadding = 16.dp
private val ItemSpacing = 8.dp
private val DaysFieldWidth = 96.dp

/**
 * Entry point of the feature. [onPaywallRequested] is the host's cue to show the paywall — this
 * module never depends on `features:paywall`, only on the fact that one exists.
 */
@Composable
public fun PantryRoute(
    onPaywallRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PantryViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PantryEffect.PaywallRequested -> onPaywallRequested()
            }
        }
    }

    PantryScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun PantryScreen(
    state: PantryState,
    onIntent: (PantryIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        HeaderText(text = "Pantry")
        AddItemRow(state = state, onIntent = onIntent)
        FreeTierNotice(state = state, onIntent = onIntent)

        if (state.isLoading) {
            Loader()
        } else {
            ItemList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun AddItemRow(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(weight = 1f),
            label = { CommentText(text = "What did you buy?") },
            value = state.newItemName,
            onValueChange = { name -> onIntent(PantryIntent.NameChanged(name)) },
        )
        OutlinedTextField(
            modifier = Modifier.width(DaysFieldWidth),
            label = { CommentText(text = "Days") },
            value = state.newItemDays,
            onValueChange = { days -> onIntent(PantryIntent.DaysChanged(days)) },
        )
        ElevatedButton(
            enabled = state.canAdd,
            onClick = { onIntent(PantryIntent.AddClicked) },
        ) {
            BodyText(text = "Add")
        }
    }
}

/**
 * The counter only appears once the free tier is nearly full — showing "20 slots left" on an empty
 * pantry advertises a limit nobody has hit yet.
 */
@Composable
private fun FreeTierNotice(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    val remaining = state.remainingFreeSlots ?: return
    if (remaining > FREE_TIER_NOTICE_THRESHOLD) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentText(
            text = if (remaining == 0) "Pantry full" else "$remaining slots left",
            color = AppotatoTheme.colors.warning,
        )
        TextButton(onClick = { onIntent(PantryIntent.UpgradeClicked) }) {
            CommentText(text = "Go Pro")
        }
    }
}

@Composable
private fun ItemList(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    if (state.entries.isEmpty()) {
        BodyText(text = "Nothing here yet. Add what is in your fridge.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(ItemSpacing)) {
        items(items = state.entries, key = { entry -> entry.item.id }) { entry ->
            PantryRow(entry = entry, onDelete = { onIntent(PantryIntent.DeleteClicked(entry.item.id)) })
        }
    }
}

@Composable
private fun PantryRow(entry: PantryEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            BodyText(text = entry.item.name)
            CommentText(text = entry.expiryLabel(), color = entry.status.color())
        }
        TextButton(onClick = onDelete) {
            CommentText(text = "Delete")
        }
    }
    Spacer(modifier = Modifier.height(ItemSpacing))
}

private const val FREE_TIER_NOTICE_THRESHOLD = 5

private fun PantryEntry.expiryLabel(): String = when {
    daysUntilExpiry < 0 -> "Expired ${-daysUntilExpiry}d ago"
    daysUntilExpiry == 0 -> "Expires today"
    else -> "Expires in ${daysUntilExpiry}d"
}

@Composable
private fun ExpiryStatus.color(): Color = when (this) {
    ExpiryStatus.Expired -> AppotatoTheme.colors.warning
    ExpiryStatus.ExpiringSoon -> AppotatoTheme.colors.info
    ExpiryStatus.Fresh -> AppotatoTheme.colors.primary
}
