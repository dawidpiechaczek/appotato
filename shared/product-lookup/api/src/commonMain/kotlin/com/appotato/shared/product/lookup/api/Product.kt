package com.appotato.shared.product.lookup.api

/**
 * What a public food database knows about one packaged product.
 *
 * Everything but [barcode] is optional, and callers have to treat it that way: these entries are
 * crowd-sourced, and a real one routinely carries a name and nothing else. The type exists to
 * pre-fill a form the user can still correct, not to be authoritative.
 */
public data class Product(
    public val barcode: String,
    /** Already includes the brand where the source has one — "Ferrero Nutella", not "Nutella". */
    public val name: String?,
    /** As printed on the pack: "500 g", "1 l". Free text, never a parsed amount. */
    public val quantity: String?,
    /**
     * A photo of the packaging, at thumbnail size — the source keeps several resolutions and this
     * is the smallest usable one, because a list row is where it gets shown.
     */
    public val imageUrl: String?,
    /**
     * Machine-readable classification tags, lower-case and language-prefixed by the source
     * (`en:dairies`). Meant for matching, never for display.
     */
    public val categoryTags: List<String>,
    public val nutrition: Nutrition?
)

/**
 * Only energy, because it is the one figure a pantry has a use for. Per 100 g is what these
 * databases carry for nearly everything; per serving exists only where the producer declared a
 * serving, so it stays separate rather than being derived from a guess.
 */
public data class Nutrition(
    public val caloriesPer100g: Int?,
    public val caloriesPerServing: Int?,
    /** The serving [caloriesPerServing] refers to, as printed: "30 g", "250 ml". */
    public val servingSize: String?
)
