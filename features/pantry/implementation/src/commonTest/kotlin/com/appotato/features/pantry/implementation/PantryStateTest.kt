package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** The derived values the screen renders straight out of state. */
class PantryStateTest {

    private val today = LocalDate(2026, 8, 12)

    private fun entry(name: String, category: ProductCategory, status: ExpiryStatus, days: Int) = PantryEntry(
        item = PantryItem(id = name, name = name, expiresOn = today, category = category),
        status = status,
        daysUntilExpiry = days
    )

    private val milk = entry("Mleko", ProductCategory.Dairy, ExpiryStatus.ExpiringSoon, 1)
    private val cheese = entry("Ser", ProductCategory.Dairy, ExpiryStatus.Fresh, 9)
    private val tomato = entry("Pomidor", ProductCategory.Vegetables, ExpiryStatus.Expired, -2)

    private val state = PantryState(isLoading = false, entries = listOf(milk, cheese, tomato))

    @Test
    fun `Given no filter When entries are read Then everything is visible`() {
        assertContentEquals(listOf(milk, cheese, tomato), state.visibleEntries)
    }

    @Test
    fun `Given a category filter When entries are read Then only that category is visible`() {
        val filtered = state.copy(categoryFilter = ProductCategory.Dairy)

        assertContentEquals(listOf(milk, cheese), filtered.visibleEntries)
    }

    @Test
    fun `Given a category with nothing in it When entries are read Then the list is empty`() {
        assertContentEquals(emptyList(), state.copy(categoryFilter = ProductCategory.Meat).visibleEntries)
    }

    /** Expired items are shown in red but are not "expiring", so the banner must not claim them. */
    @Test
    fun `Given expired and expiring items When the banner counts Then only expiring ones count`() {
        assertEquals(1, state.expiringSoonCount)
    }

    @Test
    fun `Given nothing is expiring When the banner counts Then it is zero`() {
        assertEquals(0, PantryState(entries = listOf(cheese)).expiringSoonCount)
    }

    @Test
    fun `Given a filter is active When free slots are counted Then the whole pantry still counts`() {
        val filtered = state.copy(categoryFilter = ProductCategory.Meat)

        assertEquals(FREE_TIER_ITEM_LIMIT - 3, filtered.remainingFreeSlots)
    }
}
