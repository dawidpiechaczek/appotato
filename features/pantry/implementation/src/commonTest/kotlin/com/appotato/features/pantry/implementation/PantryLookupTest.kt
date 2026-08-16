package com.appotato.features.pantry.implementation

import com.appotato.shared.billing.fake.BillingFake
import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.product.lookup.api.Nutrition
import com.appotato.shared.product.lookup.api.Product
import com.appotato.shared.product.lookup.api.ProductLookup
import com.appotato.shared.product.lookup.fake.ProductLookupFake
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull

/** What a scanned barcode does to the add form once the product database has answered. */
@OptIn(ExperimentalCoroutinesApi::class)
class PantryLookupTest {

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
    private val fakeLookup = ProductLookupFake()

    /** Swapped out by the one test that needs the lookup to still be in flight. */
    private var productLookup: ProductLookup = fakeLookup

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pantry() = PantryViewModel(repository, billing, pendingScan, productLookup, { today }, dispatchers)

    private fun milk() = Product(
        barcode = BARCODE,
        name = "Łaciate Mleko 3,2%",
        quantity = "1 l",
        imageUrl = MILK_PHOTO,
        categoryTags = listOf("en:beverages", "en:dairies", "en:milks"),
        nutrition = Nutrition(caloriesPer100g = 61, caloriesPerServing = null, servingSize = null)
    )

    @Test
    fun `Given a scan When the product is known Then the form is filled in from it`() = runTest(dispatcher) {
        fakeLookup.result = Result.success(milk())
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()

        val state = pantry.state.value
        assertEquals(LookupStatus.Found, state.lookup)
        assertEquals("Łaciate Mleko 3,2%", state.newItemName)
        assertEquals("1 l", state.newItemQuantity)
        assertEquals("61", state.newItemCalories)
        assertEquals(ProductCategory.Dairy, state.newItemCategory)
        assertEquals(listOf(BARCODE), fakeLookup.requestedBarcodes)
    }

    @Test
    fun `Given a scan When the lookup is still running Then the form already has the code`() = runTest(dispatcher) {
        val answered = CompletableDeferred<Unit>()
        productLookup = object : ProductLookup {
            override suspend fun byBarcode(barcode: String): Result<Product?> {
                answered.await()
                return Result.success(milk())
            }
        }
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()

        assertEquals(LookupStatus.InProgress, pantry.state.value.lookup)
        assertEquals(BARCODE, pantry.state.value.newItemBarcode)

        answered.complete(Unit)
        advanceUntilIdle()

        assertEquals(LookupStatus.Found, pantry.state.value.lookup)
    }

    /**
     * The camera fires on every frame it can read the label in. Each of those reaches the scanner
     * as its own scan, because the hand-off latch has already re-opened by then.
     */
    @Test
    fun `Given the camera reporting one label repeatedly Then only the first read is looked up`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            pantry()
            advanceUntilIdle()
            val scanner = ScannerViewModel(pendingScan)

            repeat(times = 20) {
                scanner.onBarcodeScanned(BARCODE)
                advanceUntilIdle()
            }

            assertEquals(listOf(BARCODE), fakeLookup.requestedBarcodes)
        }

    @Test
    fun `Given a different product When it is scanned next Then it is looked up straight away`() = runTest(dispatcher) {
        fakeLookup.result = Result.success(milk())
        pantry()
        advanceUntilIdle()
        val scanner = ScannerViewModel(pendingScan)

        scanner.onBarcodeScanned(BARCODE)
        advanceUntilIdle()
        scanner.onBarcodeScanned(OTHER_BARCODE)
        advanceUntilIdle()

        assertEquals(listOf(BARCODE, OTHER_BARCODE), fakeLookup.requestedBarcodes)
    }

    /** Buying two of the same thing is ordinary; the guard must not outlive the form it guards. */
    @Test
    fun `Given the scanned item was saved When the same product is scanned again Then it is looked up`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            val pantry = pantry()
            advanceUntilIdle()
            val scanner = ScannerViewModel(pendingScan)

            scanner.onBarcodeScanned(BARCODE)
            advanceUntilIdle()
            pantry.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()
            scanner.onBarcodeScanned(BARCODE)
            advanceUntilIdle()

            assertEquals(listOf(BARCODE, BARCODE), fakeLookup.requestedBarcodes)
        }

    @Test
    fun `Given the sheet was dismissed When the same product is scanned again Then it is looked up`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            val pantry = pantry()
            advanceUntilIdle()
            val scanner = ScannerViewModel(pendingScan)

            scanner.onBarcodeScanned(BARCODE)
            advanceUntilIdle()
            pantry.onIntent(PantryIntent.AddSheetDismissed)
            scanner.onBarcodeScanned(BARCODE)
            advanceUntilIdle()

            assertEquals(listOf(BARCODE, BARCODE), fakeLookup.requestedBarcodes)
        }

    @Test
    fun `Given a scan When the code is not in the database Then the form is left to the user`() = runTest(dispatcher) {
        fakeLookup.result = Result.success(null)
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()

        assertEquals(LookupStatus.NotFound, pantry.state.value.lookup)
        assertEquals("", pantry.state.value.newItemName)
        assertEquals(BARCODE, pantry.state.value.newItemBarcode)
    }

    @Test
    fun `Given a scan When the lookup fails Then the failure is shown and the code kept`() = runTest(dispatcher) {
        fakeLookup.result = Result.failure(IllegalStateException("offline"))
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()

        assertEquals(LookupStatus.Failed, pantry.state.value.lookup)
        assertEquals(BARCODE, pantry.state.value.newItemBarcode)
    }

    @Test
    fun `Given the user typed a name during the lookup When it answers Then their name is kept`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            val pantry = pantry()
            advanceUntilIdle()

            ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
            pantry.onIntent(PantryIntent.NameChanged("Mleko od sąsiada"))
            advanceUntilIdle()

            assertEquals("Mleko od sąsiada", pantry.state.value.newItemName)
            // The fields they did not touch are still filled in.
            assertEquals("61", pantry.state.value.newItemCalories)
        }

    @Test
    fun `Given a filled-in form When the item is saved Then the calories are stored with it`() = runTest(dispatcher) {
        fakeLookup.result = Result.success(milk())
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()
        pantry.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        val stored = repository.current.single()
        assertEquals(61, stored.caloriesPer100g)
        assertEquals(MILK_PHOTO, stored.imageUrl)
        assertEquals(BARCODE, stored.barcode)
    }

    @Test
    fun `Given calories typed by hand When the item is saved Then they are stored`() = runTest(dispatcher) {
        val pantry = pantry()
        advanceUntilIdle()

        pantry.onIntent(PantryIntent.NameChanged("Ser"))
        pantry.onIntent(PantryIntent.CaloriesChanged("330"))
        pantry.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals(330, repository.current.single().caloriesPer100g)
    }

    @Test
    fun `Given no calories at all When the item is saved Then the figure stays unknown`() = runTest(dispatcher) {
        val pantry = pantry()
        advanceUntilIdle()

        pantry.onIntent(PantryIntent.NameChanged("Ser"))
        pantry.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertNull(repository.current.single().caloriesPer100g)
    }

    @Test
    fun `Given a scanned item When it is saved Then the next one starts without the scan`() = runTest(dispatcher) {
        fakeLookup.result = Result.success(milk())
        val pantry = pantry()
        advanceUntilIdle()

        ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
        advanceUntilIdle()
        pantry.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        val state = pantry.state.value
        assertNull(state.newItemBarcode)
        assertEquals("", state.newItemCalories)
        assertNull(state.newItemImageUrl)
        assertEquals(LookupStatus.Idle, state.lookup)
    }

    @Test
    fun `Given a scan and a renamed item When it is saved Then the scan's ingredient still wins`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            val pantry = pantry()
            advanceUntilIdle()

            ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
            advanceUntilIdle()
            // The user renames it to something the name matcher would read as cheese. The barcode
            // says what is in the carton, and the barcode is the better witness.
            pantry.onIntent(PantryIntent.NameChanged("Serek do lodówki"))
            pantry.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertEquals("milk", repository.current.single().ingredientCode)
        }

    @Test
    fun `Given a scan the user backs out of When the sheet is dismissed Then the code is dropped`() =
        runTest(dispatcher) {
            fakeLookup.result = Result.success(milk())
            val pantry = pantry()
            advanceUntilIdle()

            ScannerViewModel(pendingScan).onBarcodeScanned(BARCODE)
            advanceUntilIdle()
            pantry.onIntent(PantryIntent.AddSheetDismissed)

            val state = pantry.state.value
            assertNull(state.newItemBarcode)
            assertEquals("", state.newItemCalories)
            assertEquals(LookupStatus.Idle, state.lookup)
        }

    private companion object {
        const val BARCODE = "5900512300306"
        const val OTHER_BARCODE = "3017620422003"
        const val MILK_PHOTO = "https://images.openfoodfacts.org/front.200.jpg"
    }
}
