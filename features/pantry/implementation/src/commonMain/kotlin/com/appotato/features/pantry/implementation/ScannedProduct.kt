package com.appotato.features.pantry.implementation

/**
 * What a barcode turned out to be, in the pantry's own terms — the add sheet's starting point, not
 * a finished item. Every field is optional because every one of them is missing from some record in
 * a crowd-sourced database, and the user can correct all of them before saving.
 */
internal data class ScannedProduct(
    val barcode: String,
    val name: String?,
    val quantity: String?,
    val caloriesPer100g: Int?,
    val imageUrl: String?,
    /** Null when the source's tags matched nothing here — the picker keeps whatever it showed. */
    val category: ProductCategory?,
    /**
     * Resolved from the source's tags, which are a machine vocabulary and so beat anything that
     * could be read off the name: a barcode says what the product *is* regardless of the language
     * printed on it. Null falls back to reading the name when the item is saved.
     */
    val ingredientCode: String?
)
