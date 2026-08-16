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
    /** Text rather than a number: the field is editable, and "" has to survive a round trip. */
    val newItemCalories: String = "",
    /** Set by the scanner, carried into the stored item, cleared once it is saved. */
    val newItemBarcode: String? = null,
    /** The photo the lookup found, if any. Not editable — there is no field to type a URL into. */
    val newItemImageUrl: String? = null,
    /**
     * What the scan's tags resolved to, held until the item is saved. Null for a hand-typed item,
     * and for a scan whose tags matched nothing — either way the name is read instead at save time.
     */
    val newItemIngredientCode: String? = null,
    val lookup: LookupStatus = LookupStatus.Idle,
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

    /** Anything that is not a plain number is stored as "unknown" rather than rejected. */
    val newItemCaloriesOrNull: Int? = newItemCalories.toIntOrNull()

    val canAdd: Boolean = newItemName.isNotBlank() && newItemDaysOrNull != null

    /** Null once the user is on Pro — the counter disappears rather than reading "20 of ∞". */
    val remainingFreeSlots: Int? =
        if (isPro) null else (FREE_TIER_ITEM_LIMIT - entries.size).coerceAtLeast(0)

    companion object {
        /** A week is the least surprising default for food someone just bought. */
        const val DefaultDaysUntilExpiry: String = "7"
    }
}

/**
 * Fills the add form in from a scan.
 *
 * Every editable field is only written while it is still empty. The lookup takes a moment, and a
 * user who started typing during it must not have the answer taken out from under their cursor. The
 * shelf life is never touched — no product database knows when the jar in this fridge goes off.
 *
 * The photo is the one unconditional write: there is no field to type one into, so there is nothing
 * of the user's to protect.
 */
internal fun PantryState.prefilledWith(product: ScannedProduct): PantryState = copy(
    lookup = LookupStatus.Found,
    newItemName = newItemName.ifBlank { product.name.orEmpty() },
    newItemQuantity = newItemQuantity.ifBlank { product.quantity.orEmpty() },
    newItemCalories = newItemCalories.ifBlank { product.caloriesPer100g?.toString().orEmpty() },
    newItemImageUrl = product.imageUrl,
    newItemCategory = product.category ?: newItemCategory,
    // Not user-editable either, so nothing of theirs is at risk of being overwritten.
    newItemIngredientCode = product.ingredientCode
)

/** Everything the scan put in the form, undone. The category and shelf life are the user's. */
internal fun PantryState.withScanCleared(): PantryState = copy(
    newItemBarcode = null,
    newItemCalories = "",
    newItemImageUrl = null,
    newItemIngredientCode = null,
    lookup = LookupStatus.Idle
)
