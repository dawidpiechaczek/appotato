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
    /** Null is "all categories" — the state the chip row starts in. */
    val categoryFilter: ProductCategory? = null,
    val isAddSheetOpen: Boolean = false,
    val newItemName: String = "",
    val newItemDays: String = DefaultDaysUntilExpiry,
    val newItemCategory: ProductCategory = ProductCategory.Other,
    val newItemQuantity: String = "",
    /** Set by the scanner, carried into the stored item, cleared once it is saved. */
    val newItemBarcode: String? = null,
    val isPro: Boolean = false
) {
    val visibleEntries: List<PantryEntry> = categoryFilter
        ?.let { filter -> entries.filter { entry -> entry.item.category == filter } }
        ?: entries

    /**
     * Drives the banner. Already-expired items are not counted: they are shown in red in the list
     * and "expiring within N days" would be a lie about them.
     */
    val expiringSoonCount: Int = entries.count { entry -> entry.status == ExpiryStatus.ExpiringSoon }

    val newItemDaysOrNull: Int? = newItemDays.toIntOrNull()

    val canAdd: Boolean = newItemName.isNotBlank() && newItemDaysOrNull != null

    /** Null once the user is on Pro — the counter disappears rather than reading "20 of ∞". */
    val remainingFreeSlots: Int? =
        if (isPro) null else (FREE_TIER_ITEM_LIMIT - entries.size).coerceAtLeast(0)

    companion object {
        /** A week is the least surprising default for food someone just bought. */
        const val DefaultDaysUntilExpiry: String = "7"
    }
}
