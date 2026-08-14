package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.data.toDomain
import com.appotato.features.pantry.implementation.data.toEntity
import com.appotato.shared.database.PantryItemEntity
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PantryItemMapperTest {

    private val item = PantryItem(id = "abc", name = "Milk", expiresOn = LocalDate(2026, 8, 12))

    @Test
    fun `Given a domain item When mapped to a row Then the date becomes an epoch day`() {
        val entity = item.toEntity()

        assertEquals("abc", entity.id)
        assertEquals("Milk", entity.name)
        assertEquals(LocalDate(2026, 8, 12).toEpochDays().toLong(), entity.expiresOnEpochDay)
    }

    @Test
    fun `Given a row When mapped back Then the item survives the round trip`() {
        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun `Given a row from before the epoch When mapped Then the date is still correct`() {
        val entity = PantryItemEntity(id = "old", name = "Wine", expiresOnEpochDay = -1L)

        assertEquals(LocalDate(1969, 12, 31), entity.toDomain().expiresOn)
    }
}
