package com.appotato.shared.product.lookup.implementation

import com.appotato.shared.product.lookup.api.Nutrition
import com.appotato.shared.product.lookup.api.Product
import kotlin.math.roundToInt

internal fun ProductDto.toDomain(barcode: String): Product = Product(
    barcode = barcode,
    name = displayName(),
    quantity = quantity?.trimmedOrNull(),
    // Smallest first: this is shown at thumbnail size, and the full image is several times the
    // weight for pixels nothing renders.
    imageUrl = smallImageUrl?.trimmedOrNull() ?: imageUrl?.trimmedOrNull(),
    categoryTags = categoryTags.map { tag -> tag.lowercase() },
    nutrition = nutriments?.toDomain(servingSize)
)

/**
 * "Ferrero Nutella" reads better on a shelf than "Nutella", but only when the two are actually
 * different words — the brand is skipped when the product name already carries it, which is the
 * common case for anything sold under its own name.
 */
private fun ProductDto.displayName(): String? {
    val product = name?.trimmedOrNull()
    val brand = brands?.substringBefore(',')?.trimmedOrNull()
    return when {
        product == null -> brand
        brand == null || product.contains(brand, ignoreCase = true) -> product
        else -> "$brand $product"
    }
}

/**
 * Null rather than an empty [Nutrition]: a record with a `nutriments` object and no energy in it is
 * the same amount of information as no record at all, and the caller should not have to unwrap two
 * levels to find that out.
 */
private fun NutrimentsDto.toDomain(servingSize: String?): Nutrition? {
    val per100g = caloriesPer100g?.roundToInt()
    val perServing = caloriesPerServing?.roundToInt()
    if (per100g == null && perServing == null) return null

    return Nutrition(
        caloriesPer100g = per100g,
        caloriesPerServing = perServing,
        // Meaningless without a per-serving figure to attach it to.
        servingSize = servingSize?.trimmedOrNull()?.takeIf { perServing != null }
    )
}

/** Empty strings come back for fields nobody filled in; they are absences, not values. */
private fun String.trimmedOrNull(): String? = trim().takeIf { it.isNotEmpty() }
