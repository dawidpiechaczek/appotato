package com.appotato.shared.product.lookup.implementation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductMapperTest {

    private fun dto(
        name: String? = "Nutella",
        brands: String? = null,
        quantity: String? = null,
        servingSize: String? = null,
        nutriments: NutrimentsDto? = null
    ) = ProductDto(
        name = name,
        brands = brands,
        quantity = quantity,
        servingSize = servingSize,
        nutriments = nutriments
    )

    @Test
    fun `Given a brand the name does not carry When it is mapped Then the two are joined`() {
        assertEquals("Ferrero Nutella", dto(name = "Nutella", brands = "Ferrero").toDomain(BARCODE).name)
    }

    @Test
    fun `Given a name that already carries the brand When it is mapped Then it is not repeated`() {
        assertEquals("Nutella", dto(name = "Nutella", brands = "nutella").toDomain(BARCODE).name)
    }

    @Test
    fun `Given several brands When it is mapped Then only the first is used`() {
        assertEquals("Ferrero Nutella", dto(name = "Nutella", brands = "Ferrero, Yum yum").toDomain(BARCODE).name)
    }

    @Test
    fun `Given a record with only a brand When it is mapped Then the brand is the name`() {
        assertEquals("Ferrero", dto(name = null, brands = "Ferrero").toDomain(BARCODE).name)
    }

    @Test
    fun `Given a record with neither When it is mapped Then it has no name`() {
        assertNull(dto(name = null, brands = null).toDomain(BARCODE).name)
    }

    @Test
    fun `Given fields left empty by their contributor When they are mapped Then they read as absent`() {
        val product = dto(name = "  ", brands = "", quantity = "   ").toDomain(BARCODE)

        assertNull(product.name)
        assertNull(product.quantity)
    }

    @Test
    fun `Given calories with a decimal When they are mapped Then they are rounded to whole ones`() {
        val nutrition = dto(nutriments = NutrimentsDto(caloriesPer100g = 42.6)).toDomain(BARCODE).nutrition

        assertEquals(43, nutrition?.caloriesPer100g)
    }

    @Test
    fun `Given a nutriments block with no energy in it When it is mapped Then there is no nutrition`() {
        assertNull(dto(nutriments = NutrimentsDto()).toDomain(BARCODE).nutrition)
    }

    @Test
    fun `Given no nutriments at all When it is mapped Then there is no nutrition`() {
        assertNull(dto(nutriments = null).toDomain(BARCODE).nutrition)
    }

    @Test
    fun `Given a declared serving When it is mapped Then it comes with the per-serving figure`() {
        val nutrition = dto(
            servingSize = "30 g",
            nutriments = NutrimentsDto(caloriesPer100g = 539.0, caloriesPerServing = 161.7)
        ).toDomain(BARCODE).nutrition

        assertEquals(162, nutrition?.caloriesPerServing)
        assertEquals("30 g", nutrition?.servingSize)
    }

    @Test
    fun `Given a serving size with no per-serving figure When it is mapped Then it is dropped`() {
        val nutrition = dto(
            servingSize = "30 g",
            nutriments = NutrimentsDto(caloriesPer100g = 539.0)
        ).toDomain(BARCODE).nutrition

        assertNull(nutrition?.servingSize)
    }

    /** Kept apart from [dto] so neither helper grows past what detekt allows for a parameter list. */
    private fun imageDto(imageUrl: String? = null, smallImageUrl: String? = null) =
        ProductDto(name = "Nutella", imageUrl = imageUrl, smallImageUrl = smallImageUrl)

    @Test
    fun `Given both image sizes When they are mapped Then the thumbnail is preferred`() {
        val product = imageDto(imageUrl = "$IMAGES/front.400.jpg", smallImageUrl = "$IMAGES/front.200.jpg")

        assertEquals("$IMAGES/front.200.jpg", product.toDomain(BARCODE).imageUrl)
    }

    @Test
    fun `Given only the full-size image When it is mapped Then it is used`() {
        assertEquals("$IMAGES/front.400.jpg", imageDto(imageUrl = "$IMAGES/front.400.jpg").toDomain(BARCODE).imageUrl)
    }

    @Test
    fun `Given a record nobody photographed When it is mapped Then there is no image`() {
        assertNull(imageDto(imageUrl = "").toDomain(BARCODE).imageUrl)
    }

    private companion object {
        const val BARCODE = "3017620422003"
        const val IMAGES = "https://images.openfoodfacts.org/images/products/301"
    }
}
