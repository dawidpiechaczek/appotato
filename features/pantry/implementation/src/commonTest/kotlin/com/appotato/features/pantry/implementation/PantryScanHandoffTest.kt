package com.appotato.features.pantry.implementation

import com.appotato.shared.billing.fake.BillingFake
import com.appotato.shared.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scanner tab and the pantry tab meet only through [PendingScan]; this is that seam. */
@OptIn(ExperimentalCoroutinesApi::class)
class PantryScanHandoffTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        main = dispatcher,
        default = dispatcher,
        io = dispatcher,
        unconfined = dispatcher
    )
    private val today = LocalDate(2026, 8, 12)
    private val billing = BillingFake()
    private val repository = PantryRepositoryFake()
    private val pendingScan = PendingScan()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pantry() = PantryViewModel(repository, billing, pendingScan, { today }, dispatchers)

    private fun scanner() = ScannerViewModel(pendingScan)

    @Test
    fun `Given nothing pending When a barcode is scanned Then it is accepted`() {
        assertTrue(scanner().onBarcodeScanned("5901234123457"))
        assertEquals("5901234123457", pendingScan.barcode.value)
    }

    @Test
    fun `Given a code is already pending When the camera reads it again Then it is rejected`() {
        val scanner = scanner()
        scanner.onBarcodeScanned("first")

        assertFalse(scanner.onBarcodeScanned("second"))
        assertEquals("first", pendingScan.barcode.value)
    }

    @Test
    fun `Given a scanned code When the pantry picks it up Then the add sheet opens with it`() = runTest(dispatcher) {
        val pantry = pantry()
        advanceUntilIdle()

        scanner().onBarcodeScanned("5901234123457")
        advanceUntilIdle()

        assertTrue(pantry.state.value.isAddSheetOpen)
        assertEquals("5901234123457", pantry.state.value.newItemBarcode)
    }

    @Test
    fun `Given the pantry took the code When it is done Then the hand-off is cleared`() = runTest(dispatcher) {
        pantry()
        advanceUntilIdle()

        scanner().onBarcodeScanned("5901234123457")
        advanceUntilIdle()

        assertNull(pendingScan.barcode.value)
    }

    @Test
    fun `Given a scanned item When it is saved Then the sheet closes and the code is not reused`() =
        runTest(dispatcher) {
            val pantry = pantry()
            advanceUntilIdle()
            scanner().onBarcodeScanned("5901234123457")
            advanceUntilIdle()

            pantry.onIntent(PantryIntent.NameChanged("Mleko"))
            pantry.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertEquals("5901234123457", repository.current.single().barcode)
            assertFalse(pantry.state.value.isAddSheetOpen)
            assertNull(pantry.state.value.newItemBarcode)
        }
}
