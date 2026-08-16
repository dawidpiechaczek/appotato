package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate

/**
 * One thing in the pantry, as the app thinks about it — a `LocalDate` and not the epoch day the
 * database stores, because "expires on the 14th" is a calendar fact and has no time of day.
 *
 * There is deliberately no `daysRemaining` field. A stored countdown is wrong the moment the clock
 * passes midnight with the app still open; it is computed into `PantryEntry` on every emission.
 */
internal data class PantryItem(
    val id: String,
    val name: String,
    val expiresOn: LocalDate,
    val category: ProductCategory = ProductCategory.Other,
    val quantity: String = "",
    val barcode: String? = null,
    /** Unknown for anything added by hand: it is filled in from a product database after a scan. */
    val caloriesPer100g: Int? = null,
    /** A photo of the packaging, when the scan found one. The card falls back to the emoji. */
    val imageUrl: String? = null
)
