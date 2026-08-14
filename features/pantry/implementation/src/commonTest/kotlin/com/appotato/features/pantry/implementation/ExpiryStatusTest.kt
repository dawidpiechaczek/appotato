package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpiryStatusTest {

    private val today = LocalDate(2026, 8, 12)

    private fun itemExpiringOn(date: LocalDate) =
        PantryItem(id = "id", name = "Milk", expiresOn = date)

    @Test
    fun `Given an item that expired yesterday When status is read Then it is expired`() {
        val status = itemExpiringOn(LocalDate(2026, 8, 11)).statusOn(today)

        assertEquals(ExpiryStatus.Expired, status)
    }

    @Test
    fun `Given an item expiring today When status is read Then it is expiring soon`() {
        val status = itemExpiringOn(today).statusOn(today)

        assertEquals(ExpiryStatus.ExpiringSoon, status)
    }

    @Test
    fun `Given an item on the last soon day When status is read Then it is still expiring soon`() {
        val status = itemExpiringOn(LocalDate(2026, 8, 15)).statusOn(today)

        assertEquals(ExpiryStatus.ExpiringSoon, status)
    }

    @Test
    fun `Given an item one day past the soon window When status is read Then it is fresh`() {
        val status = itemExpiringOn(LocalDate(2026, 8, 16)).statusOn(today)

        assertEquals(ExpiryStatus.Fresh, status)
    }

    @Test
    fun `Given an item expiring next month When status is read Then it is fresh`() {
        val status = itemExpiringOn(LocalDate(2026, 9, 1)).statusOn(today)

        assertEquals(ExpiryStatus.Fresh, status)
    }
}
