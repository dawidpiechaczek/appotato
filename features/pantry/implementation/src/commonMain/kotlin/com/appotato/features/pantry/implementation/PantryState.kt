package com.appotato.features.pantry.implementation

import androidx.compose.runtime.Immutable

/** How many items the free tier holds before the paywall appears. */
internal const val FREE_TIER_ITEM_LIMIT: Int = 20

/**
 * An item with everything the row needs already worked out, so the Composable never has to know
 * what day it is.
 */
@Immutable
internal data class PantryEntry(
    val item: PantryItem,
    val status: ExpiryStatus,
    val daysUntilExpiry: Int
)

@Immutable
internal data class PantryState(
    val isLoading: Boolean = true,
    val entries: List<PantryEntry> = emptyList(),
    val newItemName: String = "",
    val newItemDays: String = DefaultDaysUntilExpiry,
    val isPro: Boolean = false
) {
    val newItemDaysOrNull: Int? = newItemDays.toIntOrNull()

    val canAdd: Boolean = newItemName.isNotBlank() && newItemDaysOrNull != null

    /** Null once the user is on Pro — the row counter disappears rather than reading "20 of ∞". */
    val remainingFreeSlots: Int? =
        if (isPro) null else (FREE_TIER_ITEM_LIMIT - entries.size).coerceAtLeast(0)

    companion object {
        /** A week is the least surprising default for food someone just bought. */
        const val DefaultDaysUntilExpiry: String = "7"
    }
}
