package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    private val scannedMilk = ScannedProduct(
        barcode = "5901234123457",
        name = "Mleko",
        quantity = "1 l",
        caloriesPer100g = 61,
        imageUrl = MILK_PHOTO,
        category = ProductCategory.Dairy
    )

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

    @Test
    fun `Given calories typed into the field When they are read Then they parse to a number`() {
        assertEquals(61, PantryState(newItemCalories = "61").newItemCaloriesOrNull)
    }

    @Test
    fun `Given a calorie field left empty or mistyped When it is read Then the figure is unknown`() {
        assertNull(PantryState(newItemCalories = "").newItemCaloriesOrNull)
        assertNull(PantryState(newItemCalories = "sporo").newItemCaloriesOrNull)
    }

    @Test
    fun `Given an untouched form When a scan answers Then every field is filled in`() {
        val prefilled = PantryState().prefilledWith(scannedMilk)

        assertEquals(LookupStatus.Found, prefilled.lookup)
        assertEquals("Mleko", prefilled.newItemName)
        assertEquals("1 l", prefilled.newItemQuantity)
        assertEquals("61", prefilled.newItemCalories)
        assertEquals(ProductCategory.Dairy, prefilled.newItemCategory)
        assertEquals(MILK_PHOTO, prefilled.newItemImageUrl)
    }

    /** The lookup takes a moment, and whatever the user typed during it is the better answer. */
    @Test
    fun `Given fields the user already typed When a scan answers Then their input stands`() {
        val typed = PantryState(
            newItemName = "Mleko od sąsiada",
            newItemQuantity = "2 l",
            newItemCalories = "70",
            newItemCategory = ProductCategory.Other
        )

        val prefilled = typed.prefilledWith(scannedMilk)

        assertEquals("Mleko od sąsiada", prefilled.newItemName)
        assertEquals("2 l", prefilled.newItemQuantity)
        assertEquals("70", prefilled.newItemCalories)
        // The category is the one field with no empty value to test against, so the guess wins.
        assertEquals(ProductCategory.Dairy, prefilled.newItemCategory)
    }

    @Test
    fun `Given a record with nothing in it When a scan answers Then the form is left alone`() {
        val empty = ScannedProduct(
            barcode = "5901234123457",
            name = null,
            quantity = null,
            caloriesPer100g = null,
            imageUrl = null,
            category = null
        )

        val prefilled = PantryState(newItemCategory = ProductCategory.Meat).prefilledWith(empty)

        assertEquals("", prefilled.newItemName)
        assertEquals("", prefilled.newItemCalories)
        assertEquals(ProductCategory.Meat, prefilled.newItemCategory)
    }

    /** Shelf life is the user's: no product database knows when the jar in this fridge goes off. */
    @Test
    fun `Given a shelf life the user set When a scan answers Then it is not touched`() {
        val prefilled = PantryState(newItemDays = "3").prefilledWith(scannedMilk)

        assertEquals("3", prefilled.newItemDays)
    }

    @Test
    fun `Given a form filled in from a scan When the scan is cleared Then only its traces go`() {
        val cleared = PantryState().prefilledWith(scannedMilk)
            .copy(newItemBarcode = "5901234123457")
            .withScanCleared()

        assertNull(cleared.newItemBarcode)
        assertEquals("", cleared.newItemCalories)
        assertNull(cleared.newItemImageUrl)
        assertEquals(LookupStatus.Idle, cleared.lookup)
        assertEquals("Mleko", cleared.newItemName)
        assertEquals(ProductCategory.Dairy, cleared.newItemCategory)
    }

    private companion object {
        const val MILK_PHOTO = "https://images.openfoodfacts.org/front.200.jpg"
    }
}
