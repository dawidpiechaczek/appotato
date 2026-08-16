package com.appotato.features.pantry.implementation

import com.appotato.shared.billing.api.Entitlement
import com.appotato.shared.billing.api.SubscriptionStatus
import com.appotato.shared.billing.fake.BillingFake
import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.product.lookup.fake.ProductLookupFake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        main = dispatcher,
        default = dispatcher,
        io = dispatcher,
        unconfined = dispatcher
    )
    private val today = LocalDate(2026, 8, 12)
    private val billing = BillingFake()
    private var repository = PantryRepositoryFake()
    private val pendingScan = PendingScan()
    private val productLookup = ProductLookupFake()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PantryViewModel(repository, billing, pendingScan, productLookup, { today }, dispatchers)

    private fun item(id: String, name: String, expiresOn: LocalDate) =
        PantryItem(id = id, name = name, expiresOn = expiresOn)

    private fun items(count: Int) = List(count) { index ->
        item("id-$index", "Item $index", today)
    }

    private fun goPro() {
        billing.emit(
            SubscriptionStatus.Active(
                planId = "pro_yearly",
                entitlements = setOf(Entitlement.Pro),
                expiresAtEpochMillis = 1L,
                willRenew = true
            )
        )
    }

    @Test
    fun `Given stored items When the screen opens Then they are shown soonest first`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(
            listOf(
                item("2", "Cheese", LocalDate(2026, 8, 20)),
                item("1", "Milk", LocalDate(2026, 8, 13))
            )
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertContentEquals(listOf("Milk", "Cheese"), viewModel.state.value.entries.map { it.item.name })
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Given an item due in two days When the screen opens Then it is marked expiring soon`() =
        runTest(dispatcher) {
            repository = PantryRepositoryFake(listOf(item("1", "Milk", LocalDate(2026, 8, 14))))
            val viewModel = viewModel()

            advanceUntilIdle()

            val entry = viewModel.state.value.entries.single()
            assertEquals(ExpiryStatus.ExpiringSoon, entry.status)
            assertEquals(2, entry.daysUntilExpiry)
        }

    @Test
    fun `Given an expired item When the screen opens Then the day count is negative`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(listOf(item("1", "Milk", LocalDate(2026, 8, 10))))
        val viewModel = viewModel()

        advanceUntilIdle()

        val entry = viewModel.state.value.entries.single()
        assertEquals(ExpiryStatus.Expired, entry.status)
        assertEquals(-2, entry.daysUntilExpiry)
    }

    @Test
    fun `Given a name and a shelf life When add is clicked Then the item is stored with that date`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.NameChanged("Yoghurt"))
            viewModel.onIntent(PantryIntent.DaysChanged("5"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            val stored = repository.current.single()
            assertEquals("Yoghurt", stored.name)
            assertEquals(LocalDate(2026, 8, 17), stored.expiresOn)
        }

    @Test
    fun `Given an item was added When the state settles Then only the name field clears`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("Yoghurt"))
        viewModel.onIntent(PantryIntent.DaysChanged("5"))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.newItemName)
        assertEquals("5", viewModel.state.value.newItemDays)
    }

    @Test
    fun `Given a hand-typed name When the item is added Then the ingredient is read off it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.NameChanged("Ser żółty Gouda"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertEquals("cheese", repository.current.single().ingredientCode)
        }

    @Test
    fun `Given a name naming no known food When the item is added Then no ingredient is invented`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.NameChanged("Zestaw upominkowy"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertNull(repository.current.single().ingredientCode)
        }

    @Test
    fun `Given a name of only spaces When add is clicked Then nothing is stored`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("   "))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertTrue(repository.current.isEmpty())
        assertFalse(viewModel.state.value.canAdd)
    }

    @Test
    fun `Given a shelf life that is not a number When add is clicked Then nothing is stored`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.NameChanged("Yoghurt"))
            viewModel.onIntent(PantryIntent.DaysChanged("soon"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertTrue(repository.current.isEmpty())
            assertFalse(viewModel.state.value.canAdd)
        }

    @Test
    fun `Given the name is trimmed When it is stored Then the surrounding spaces are gone`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("  Yoghurt  "))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals("Yoghurt", repository.current.single().name)
    }

    @Test
    fun `Given an item in the list When delete is clicked Then it is removed`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(listOf(item("1", "Milk", today)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.DeleteClicked("1"))
        advanceUntilIdle()

        assertTrue(repository.current.isEmpty())
        assertTrue(viewModel.state.value.entries.isEmpty())
    }

    @Test
    fun `Given a full free pantry When add is clicked Then the paywall is requested instead`() =
        runTest(dispatcher) {
            repository = PantryRepositoryFake(items(FREE_TIER_ITEM_LIMIT))
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.NameChanged("One too many"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertEquals(PantryEffect.PaywallRequested, viewModel.effects.first())
            assertEquals(FREE_TIER_ITEM_LIMIT, repository.current.size)
        }

    @Test
    fun `Given one free slot left When add is clicked Then the item still goes in`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(items(FREE_TIER_ITEM_LIMIT - 1))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("Last one"))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals(FREE_TIER_ITEM_LIMIT, repository.current.size)
    }

    @Test
    fun `Given a Pro subscription When the pantry is full Then adding still works`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(items(FREE_TIER_ITEM_LIMIT))
        goPro()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("Twenty one"))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals(FREE_TIER_ITEM_LIMIT + 1, repository.current.size)
    }

    @Test
    fun `Given a free user When the state is read Then the remaining slots are counted`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(items(18))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.remainingFreeSlots)
        assertFalse(viewModel.state.value.isPro)
    }

    @Test
    fun `Given a full free pantry When the state is read Then no slots remain`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(items(FREE_TIER_ITEM_LIMIT + 2))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.remainingFreeSlots)
    }

    @Test
    fun `Given the user upgrades mid session When the entitlement arrives Then the limit disappears`() =
        runTest(dispatcher) {
            repository = PantryRepositoryFake(items(5))
            val viewModel = viewModel()
            advanceUntilIdle()

            goPro()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPro)
            assertNull(viewModel.state.value.remainingFreeSlots)
        }

    @Test
    fun `Given a category is picked When the item is stored Then it carries that category`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("Mleko"))
        viewModel.onIntent(PantryIntent.QuantityChanged("1 l"))
        viewModel.onIntent(PantryIntent.CategorySelected(ProductCategory.Dairy))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        val stored = repository.current.single()
        assertEquals(ProductCategory.Dairy, stored.category)
        assertEquals("1 l", stored.quantity)
    }

    @Test
    fun `Given an item was added When the form resets Then the category is kept for the next one`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PantryIntent.CategorySelected(ProductCategory.Dairy))
            viewModel.onIntent(PantryIntent.NameChanged("Mleko"))
            viewModel.onIntent(PantryIntent.AddClicked)
            advanceUntilIdle()

            assertEquals(ProductCategory.Dairy, viewModel.state.value.newItemCategory)
            assertEquals("", viewModel.state.value.newItemName)
        }

    @Test
    fun `Given a quantity of only spaces When the item is stored Then it is empty`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.NameChanged("Mleko"))
        viewModel.onIntent(PantryIntent.QuantityChanged("   "))
        viewModel.onIntent(PantryIntent.AddClicked)
        advanceUntilIdle()

        assertEquals("", repository.current.single().quantity)
    }

    @Test
    fun `Given a category filter is chosen When the list is read Then only it is visible`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(
            listOf(
                item("1", "Mleko", today).copy(category = ProductCategory.Dairy),
                item("2", "Pomidor", today).copy(category = ProductCategory.Vegetables)
            )
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.CategoryFilterSelected(ProductCategory.Dairy))

        assertContentEquals(listOf("Mleko"), viewModel.state.value.visibleEntries.map { it.item.name })
    }

    @Test
    fun `Given a filter is active When it is cleared Then everything is visible again`() = runTest(dispatcher) {
        repository = PantryRepositoryFake(listOf(item("1", "Mleko", today)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onIntent(PantryIntent.CategoryFilterSelected(ProductCategory.Meat))

        viewModel.onIntent(PantryIntent.CategoryFilterSelected(null))

        assertNull(viewModel.state.value.categoryFilter)
        assertEquals(1, viewModel.state.value.visibleEntries.size)
    }

    @Test
    fun `Given the add action When it is opened and dismissed Then the sheet follows`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.AddSheetOpened)
        assertTrue(viewModel.state.value.isAddSheetOpen)

        viewModel.onIntent(PantryIntent.AddSheetDismissed)
        assertFalse(viewModel.state.value.isAddSheetOpen)
    }

    @Test
    fun `Given the upgrade link When it is clicked Then the paywall is requested`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PantryIntent.UpgradeClicked)
        advanceUntilIdle()

        assertEquals(PantryEffect.PaywallRequested, viewModel.effects.first())
    }
}
