package com.appotato.shared.product.lookup.implementation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The shape of `/api/v2/product/{barcode}`.
 *
 * Every field is optional with a default. These records are filled in by volunteers one field at a
 * time, so "present but empty" and "absent" are both normal, and a required field here would turn
 * an incomplete entry into a parse failure.
 */
@Serializable
internal data class ProductResponseDto(
    val code: String? = null,
    /**
     * Absent when the code is unknown. Its presence — not the `status` flag next to it — is what
     * the lookup keys on, because it is the thing that actually has to be mapped.
     */
    val product: ProductDto? = null
)

@Serializable
internal data class ProductDto(
    @SerialName("product_name")
    val name: String? = null,
    /** Comma-separated and unordered: "Nutella, Ferrero, Yum yum". Only the first is usable. */
    val brands: String? = null,
    val quantity: String? = null,
    /** ~400 px. The fallback: not every record has a small version generated. */
    @SerialName("image_url")
    val imageUrl: String? = null,
    /** ~200 px, which is what a list thumbnail actually needs. */
    @SerialName("image_small_url")
    val smallImageUrl: String? = null,
    @SerialName("categories_tags")
    val categoryTags: List<String> = emptyList(),
    @SerialName("serving_size")
    val servingSize: String? = null,
    val nutriments: NutrimentsDto? = null
)

/**
 * The nutriments object carries around fifty keys; these are the two the app has a use for.
 *
 * They are `Double` because the source stores them that way — 539 comes back as `539.0`, and an
 * `Int` field would fail to parse the moment a value has a decimal point.
 */
@Serializable
internal data class NutrimentsDto(
    @SerialName("energy-kcal_100g")
    val caloriesPer100g: Double? = null,
    @SerialName("energy-kcal_serving")
    val caloriesPerServing: Double? = null
)
