package com.appotato.features.pantry.implementation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.pantry_add_open
import com.appotato.features.pantry.implementation.generated.resources.pantry_empty
import com.appotato.features.pantry.implementation.generated.resources.pantry_empty_filtered
import com.appotato.features.pantry.implementation.generated.resources.pantry_expiring_banner
import com.appotato.features.pantry.implementation.generated.resources.pantry_filter_all
import com.appotato.features.pantry.implementation.generated.resources.pantry_free_full
import com.appotato.features.pantry.implementation.generated.resources.pantry_free_slots_left
import com.appotato.features.pantry.implementation.generated.resources.pantry_go_pro
import com.appotato.features.pantry.implementation.generated.resources.pantry_subtitle_count
import com.appotato.features.pantry.implementation.generated.resources.pantry_title
import com.appotato.shared.ui.components.AppotatoIcon
import com.appotato.shared.ui.components.AppotatoTheme
import com.appotato.shared.ui.components.Banner
import com.appotato.shared.ui.components.BodyText
import com.appotato.shared.ui.components.Chip
import com.appotato.shared.ui.components.CircularAction
import com.appotato.shared.ui.components.CommentText
import com.appotato.shared.ui.components.Loader
import com.appotato.shared.ui.components.ScreenHeader
import com.appotato.shared.ui.components.TextButton
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val HorizontalPadding = 16.dp
private val ItemSpacing = 8.dp
private const val FREE_TIER_NOTICE_THRESHOLD = 5

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
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(Res.string.pantry_title),
            subtitle = pluralStringResource(
                Res.plurals.pantry_subtitle_count,
                state.entries.size,
                state.entries.size,
            ),
            action = {
                CircularAction(
                    icon = AppotatoIcon.Add,
                    contentDescription = stringResource(Res.string.pantry_add_open),
                    onClick = { onIntent(PantryIntent.AddSheetOpened) },
                )
            },
        )

        Column(
            modifier = Modifier.padding(horizontal = HorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            ExpiringBanner(count = state.expiringSoonCount)
            FreeTierNotice(state = state, onIntent = onIntent)
            CategoryFilters(selected = state.categoryFilter, onSelected = { category ->
                onIntent(PantryIntent.CategoryFilterSelected(category))
            })
        }

        when {
            state.isLoading -> Loader(modifier = Modifier.padding(HorizontalPadding))
            state.visibleEntries.isEmpty() -> EmptyState(isFiltered = state.categoryFilter != null)
            else -> ItemList(state = state, onIntent = onIntent)
        }
    }

    if (state.isAddSheetOpen) {
        AddItemSheet(
            state = state,
            onIntent = onIntent,
            onDismiss = { onIntent(PantryIntent.AddSheetDismissed) },
        )
    }
}

@Composable
private fun ExpiringBanner(count: Int) {
    if (count == 0) return
    Banner(
        modifier = Modifier.fillMaxWidth(),
        text = pluralStringResource(Res.plurals.pantry_expiring_banner, count, count),
        color = AppotatoTheme.colors.caution,
    )
}

/**
 * Scrolls horizontally rather than wrapping: six categories plus "all" do not fit on a phone, and
 * a second row of chips pushes the list itself below the fold.
 */
@Composable
private fun CategoryFilters(selected: ProductCategory?, onSelected: (ProductCategory?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        Chip(
            text = stringResource(Res.string.pantry_filter_all),
            isSelected = selected == null,
            onClick = { onSelected(null) },
        )
        ProductCategory.entries.forEach { category ->
            Chip(
                text = stringResource(category.label),
                isSelected = category == selected,
                onClick = { onSelected(category) },
            )
        }
    }
}

/**
 * The counter only appears once the free tier is nearly full — announcing "20 slots left" on an
 * empty pantry advertises a limit nobody has hit yet.
 */
@Composable
private fun FreeTierNotice(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    val remaining = state.remainingFreeSlots ?: return
    if (remaining > FREE_TIER_NOTICE_THRESHOLD) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentText(
            text = if (remaining == 0) {
                stringResource(Res.string.pantry_free_full)
            } else {
                pluralStringResource(Res.plurals.pantry_free_slots_left, remaining, remaining)
            },
            color = AppotatoTheme.colors.caution,
        )
        TextButton(onClick = { onIntent(PantryIntent.UpgradeClicked) }) {
            CommentText(text = stringResource(Res.string.pantry_go_pro))
        }
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) = BodyText(
    modifier = Modifier.padding(HorizontalPadding),
    text = if (isFiltered) {
        stringResource(Res.string.pantry_empty_filtered)
    } else {
        stringResource(Res.string.pantry_empty)
    },
)

@Composable
private fun ItemList(state: PantryState, onIntent: (PantryIntent) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        contentPadding = PaddingValues(all = HorizontalPadding),
    ) {
        items(items = state.visibleEntries, key = { entry -> entry.item.id }) { entry ->
            PantryCard(entry = entry, onDelete = { onIntent(PantryIntent.DeleteClicked(entry.item.id)) })
        }
    }
}
