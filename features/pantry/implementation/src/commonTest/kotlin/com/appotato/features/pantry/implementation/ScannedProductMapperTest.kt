package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.data.toScannedProduct
import com.appotato.shared.product.lookup.api.Nutrition
import com.appotato.shared.product.lookup.api.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScannedProductMapperTest {

    private fun product(
        name: String? = "Mleko",
        quantity: String? = "1 l",
        tags: List<String> = emptyList(),
        nutrition: Nutrition? = null,
        imageUrl: String? = null
    ) = Product(
        barcode = "5901234123457",
        name = name,
        quantity = quantity,
        imageUrl = imageUrl,
        categoryTags = tags,
        nutrition = nutrition
    )

    private fun categoryOf(vararg tags: String) = product(tags = tags.toList()).toScannedProduct().category

    @Test
    fun `Given a looked-up product When it is mapped Then the form gets what it needs`() {
        val scanned = product(nutrition = Nutrition(caloriesPer100g = 42, null, null)).toScannedProduct()

        assertEquals(
            ScannedProduct(
                barcode = "5901234123457",
                name = "Mleko",
                quantity = "1 l",
                caloriesPer100g = 42,
                imageUrl = null,
                category = null,
                ingredientCode = null
            ),
            scanned
        )
    }

    @Test
    fun `Given only a per-serving figure When it is mapped Then no calories are carried over`() {
        val nutrition = Nutrition(caloriesPer100g = null, caloriesPerServing = 161, servingSize = "30 g")

        assertNull(product(nutrition = nutrition).toScannedProduct().caloriesPer100g)
    }

    @Test
    fun `Given tags for each category When they are mapped Then each one is recognised`() {
        assertEquals(ProductCategory.Dairy, categoryOf("en:dairies", "en:yogurts"))
        assertEquals(ProductCategory.Meat, categoryOf("en:meats", "en:chickens"))
        assertEquals(ProductCategory.Beverages, categoryOf("en:beverages", "en:sodas"))
        assertEquals(ProductCategory.Vegetables, categoryOf("en:vegetables", "en:tomatoes"))
        assertEquals(ProductCategory.Fruit, categoryOf("en:fruits", "en:apples"))
    }

    /** The tags run general to specific, and the specific end is the one that describes the thing. */
    @Test
    fun `Given a fruit tagged as plant-based food and beverages When mapped Then it is not a drink`() {
        val category = categoryOf("en:plant-based-foods-and-beverages", "en:plant-based-foods", "en:fruits")

        assertEquals(ProductCategory.Fruit, category)
    }

    @Test
    fun `Given a fruit juice When it is mapped Then it is a drink rather than fruit`() {
        assertEquals(ProductCategory.Beverages, categoryOf("en:beverages", "en:fruit-juices"))
    }

    @Test
    fun `Given milk tagged as a beverage When it is mapped Then dairy wins`() {
        assertEquals(ProductCategory.Dairy, categoryOf("en:beverages", "en:dairies", "en:milks"))
    }

    @Test
    fun `Given tags this app has no category for When they are mapped Then it does not guess`() {
        assertNull(categoryOf("en:breakfasts", "en:spreads", "en:sweet-spreads"))
    }

    @Test
    fun `Given a record with no tags at all When it is mapped Then it does not guess`() {
        assertNull(categoryOf())
    }

    @Test
    fun `Given tags naming a specific food When it is mapped Then the ingredient comes with it`() {
        val scanned = product(tags = listOf("en:dairies", "en:milks")).toScannedProduct()

        assertEquals("milk", scanned.ingredientCode)
    }

    @Test
    fun `Given tags too general to name a food When it is mapped Then the ingredient is null`() {
        // The category is still worth guessing from these; the specific ingredient is not.
        val scanned = product(tags = listOf("en:dairies")).toScannedProduct()

        assertNull(scanned.ingredientCode)
    }

    @Test
    fun `Given a record missing everything but its code When it is mapped Then it maps to nulls`() {
        val scanned = product(name = null, quantity = null).toScannedProduct()

        assertNull(scanned.name)
        assertNull(scanned.quantity)
        assertNull(scanned.caloriesPer100g)
        assertNull(scanned.imageUrl)
        assertEquals("5901234123457", scanned.barcode)
    }

    private companion object {
        const val PHOTO = "https://images.openfoodfacts.org/front.200.jpg"
    }
}
