package com.appotato.features.pantry.implementation.data

import com.appotato.features.pantry.implementation.ProductCategory
import com.appotato.features.pantry.implementation.ScannedProduct
import com.appotato.shared.product.lookup.api.Product

internal fun Product.toScannedProduct(): ScannedProduct = ScannedProduct(
    barcode = barcode,
    name = name,
    quantity = quantity,
    // Per 100 g only. A per-serving figure without the serving next to it is a number the user
    // cannot check, and the two would be indistinguishable once stored in one column.
    caloriesPer100g = nutrition?.caloriesPer100g,
    imageUrl = imageUrl,
    category = categoryFor(categoryTags)
)

/**
 * The source's tags are a deep taxonomy — a jar of Nutella carries four of them — and this app has
 * six categories. The match is deliberately coarse and admits defeat by returning null.
 *
 * Two rules make it behave:
 *
 * 1. **Most specific tag first.** The tags arrive general to specific, and the specific end is the
 *    honest one: an apple is tagged `en:plant-based-foods-and-beverages` before `en:fruits`, and
 *    reading left to right would file it under drinks.
 * 2. **Within one tag, [PRIORITY] decides.** `en:fruit-juices` matches both fruit and drinks, and
 *    juice belongs with the drinks; milk is tagged as a beverage but belongs with the dairy.
 */
private fun categoryFor(tags: List<String>): ProductCategory? = tags
    .asReversed()
    .firstNotNullOfOrNull { tag -> PRIORITY.firstOrNull { (_, words) -> words.any { it in tag } } }
    ?.first

/**
 * Ordered, and the order is the rule above. Words are matched as substrings of a whole tag, so
 * `milk` also catches `en:whole-milks` — and singulars are listed because the taxonomy pluralises
 * inconsistently.
 */
private val PRIORITY: List<Pair<ProductCategory, List<String>>> = listOf(
    ProductCategory.Dairy to listOf("dairy", "dairies", "milk", "cheese", "yogurt", "yoghurt", "butter", "cream"),
    ProductCategory.Meat to listOf("meat", "poultry", "chicken", "beef", "pork", "sausage", "ham", "fish", "seafood"),
    ProductCategory.Beverages to listOf("beverage", "drink", "juice", "water", "soda", "tea", "coffee"),
    ProductCategory.Vegetables to listOf("vegetable", "legume", "potato", "tomato", "salad", "mushroom"),
    ProductCategory.Fruit to listOf("fruit", "berries", "berry", "apple", "banana", "citrus")
)
