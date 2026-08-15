package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.generated.resources.Res
import com.appotato.features.pantry.implementation.generated.resources.category_beverages
import com.appotato.features.pantry.implementation.generated.resources.category_dairy
import com.appotato.features.pantry.implementation.generated.resources.category_fruit
import com.appotato.features.pantry.implementation.generated.resources.category_meat
import com.appotato.features.pantry.implementation.generated.resources.category_other
import com.appotato.features.pantry.implementation.generated.resources.category_vegetables
import org.jetbrains.compose.resources.StringResource

/**
 * What kind of thing an item is. The emoji stands in for an icon set on purpose — it renders
 * identically on both platforms, needs no asset pipeline, and scales with the text.
 *
 * [code] is what goes in the database and must never change; the enum name and the label may.
 */
internal enum class ProductCategory(
    val code: String,
    val icon: String,
    val label: StringResource
) {
    Dairy("dairy", "🥛", Res.string.category_dairy),
    Vegetables("vegetables", "🥦", Res.string.category_vegetables),
    Meat("meat", "🥩", Res.string.category_meat),
    Fruit("fruit", "🍎", Res.string.category_fruit),
    Beverages("beverages", "🧃", Res.string.category_beverages),
    Other("other", "📦", Res.string.category_other);

    internal companion object {
        /**
         * Unknown codes fall back rather than throwing: a row written by a newer build must not be
         * able to crash an older one.
         */
        fun fromCode(code: String): ProductCategory =
            entries.firstOrNull { category -> category.code == code } ?: Other
    }
}
