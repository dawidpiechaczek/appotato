package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.data.toDomain
import com.appotato.features.pantry.implementation.data.toEntity
import com.appotato.shared.database.PantryItemEntity
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PantryItemMapperTest {

    private val item = PantryItem(
        id = "abc",
        name = "Milk",
        expiresOn = LocalDate(2026, 8, 12),
        category = ProductCategory.Dairy,
        quantity = "1 l",
        barcode = "5901234123457",
        caloriesPer100g = 42,
        imageUrl = "https://images.openfoodfacts.org/front.200.jpg"
    )

    @Test
    fun `Given a domain item When mapped to a row Then the date becomes an epoch day`() {
        val entity = item.toEntity()

        assertEquals("abc", entity.id)
        assertEquals("Milk", entity.name)
        assertEquals(LocalDate(2026, 8, 12).toEpochDays(), entity.expiresOnEpochDay)
        assertEquals("dairy", entity.category)
        assertEquals("1 l", entity.quantity)
        assertEquals("5901234123457", entity.barcode)
        assertEquals(42, entity.caloriesPer100g)
        assertEquals("https://images.openfoodfacts.org/front.200.jpg", entity.imageUrl)
    }

    @Test
    fun `Given a row When mapped back Then the item survives the round trip`() {
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun `Given a row from before the epoch When mapped Then the date is still correct`() {
        val entity = PantryItemEntity(
            id = "old",
            name = "Wine",
            expiresOnEpochDay = -1L,
            category = "other",
            quantity = ""
        )

        assertEquals(LocalDate(1969, 12, 31), entity.toDomain().expiresOn)
    }

    @Test
    fun `Given a category code a newer build wrote When mapped Then it falls back instead of crashing`() {
        val entity = PantryItemEntity(
            id = "future",
            name = "Something",
            expiresOnEpochDay = 0L,
            category = "frozen_desserts",
            quantity = ""
        )

        assertEquals(ProductCategory.Other, entity.toDomain().category)
    }
}
