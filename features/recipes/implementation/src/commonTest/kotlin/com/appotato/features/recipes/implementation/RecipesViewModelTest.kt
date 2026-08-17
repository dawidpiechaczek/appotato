package com.appotato.features.recipes.implementation

import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.recipe.source.fake.RecipeSourceFake
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        main = dispatcher,
        default = dispatcher,
        io = dispatcher,
        unconfined = dispatcher
    )
    private val today = LocalDate(2026, 8, 17)
    private val source = RecipeSourceFake()

    private class ExpiringItemsFake(var items: List<ExpiringItem> = emptyList()) : ExpiringItems {
        var calls: Int = 0
            private set
        var lastWindow: Int? = null
            private set

        override suspend fun soonestFirst(today: LocalDate, limit: Int): List<ExpiringItem> {
            calls++
            lastWindow = limit
            return items
        }
    }

    private val expiring = ExpiringItemsFake()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = RecipesViewModel(expiring, source, { today }, dispatchers)

    private fun item(name: String, code: String? = null, days: Int = 1) =
        ExpiringItem(id = name, name = name, ingredientCode = code, daysUntilExpiry = days)

    private fun recipe(title: String) = Recipe(
        title = title,
        summary = "",
        usesIngredients = listOf("Mleko"),
        missingIngredients = emptyList(),
        steps = listOf("Wymieszaj."),
        minutes = 10
    )

    @Test
    fun `Given expiring items When the tab is shown Then suggestions are requested for them`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", "milk", 2), item("Jajka", null, 5))
            source.result = Result.success(listOf(recipe("Naleśniki")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            val request = source.requests.single()
            assertEquals("pl", request.languageTag)
            assertContentEquals(listOf("Mleko", "Jajka"), request.ingredients.map { it.displayName })
            // The code rides along with the name — it is what lets the backend cache two spellings
            // of one food together, and null is a legitimate value for something unrecognised.
            assertContentEquals(listOf("milk", null), request.ingredients.map { it.code })
            assertContentEquals(listOf(2, 5), request.ingredients.map { it.daysUntilExpiry })
        }

    @Test
    fun `Given a successful answer When it arrives Then the recipes and their basis are shown`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", "milk", 2))
            source.result = Result.success(listOf(recipe("Naleśniki")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertContentEquals(listOf("Naleśniki"), state.recipes.map { it.title })
            assertContentEquals(listOf("Mleko"), state.basedOn)
            assertFalse(state.isLoading)
            assertTrue(state.hasLoaded)
            assertNull(state.failure)
        }

    @Test
    fun `Given the tab is shown again When nothing changed Then no second request is made`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", "milk", 2))
            source.result = Result.success(listOf(recipe("Naleśniki")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()
            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            // Suggestions are generated and cost tokens; a tab switch is not a reason to buy more.
            assertEquals(1, source.requests.size)
        }

    @Test
    fun `Given a refresh is asked for When it runs Then a new request is made`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", "milk", 2))
            source.result = Result.success(listOf(recipe("Naleśniki")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()
            viewModel.onIntent(RecipesIntent.RefreshClicked)
            advanceUntilIdle()

            assertEquals(2, source.requests.size)
        }

    @Test
    fun `Given nothing is expiring When the tab is shown Then nothing is requested`() =
        runTest(dispatcher) {
            expiring.items = emptyList()
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            // An empty pantry already has its answer; asking would spend a token to be told so.
            assertTrue(source.requests.isEmpty())
            assertTrue(viewModel.state.value.isEmptyPantry)
        }

    @Test
    fun `Given a full pantry with nothing urgent When shown Then no suggestions are requested`() =
        runTest(dispatcher) {
            // Plenty to cook with, but none of it is going off — so there is nothing to say, and
            // no reason to pay for a generation. This is what keeps the tab from nagging about
            // food that has a fortnight left.
            expiring.items = listOf(item("Mleko", days = 9), item("Jajka", days = 14))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            assertTrue(source.requests.isEmpty())
            assertTrue(viewModel.state.value.isEmptyPantry)
        }

    @Test
    fun `Given one urgent item When shown Then the whole pantry is sent to build around it`() =
        runTest(dispatcher) {
            expiring.items = listOf(
                item("Mleko", days = 2),
                item("Jajka", days = 9),
                item("Ser", days = 20)
            )
            source.result = Result.success(listOf(recipe("Omlet")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            // One item triggers it, but all three go out: the model is told to build around what
            // expires first, and a lone carton of milk is not a recipe.
            assertContentEquals(
                listOf("Mleko", "Jajka", "Ser"),
                source.requests.single().ingredients.map { it.displayName }
            )
            assertEquals(1, viewModel.state.value.urgentCount)
        }

    @Test
    fun `Given the trigger boundary When an item sits exactly on it Then it still counts`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", days = RECIPE_TRIGGER_DAYS))
            source.result = Result.success(listOf(recipe("Omlet")))
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            assertEquals(1, source.requests.size)
        }

    @Test
    fun `Given the request fails When it returns Then the failure is shown`() = runTest(dispatcher) {
        expiring.items = listOf(item("Mleko", "milk", 2))
        source.result = Result.failure(IllegalStateException("no endpoint"))
        val viewModel = viewModel()

        viewModel.onIntent(RecipesIntent.Shown("pl"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(RecipesFailure.Unavailable, state.failure)
        assertFalse(state.isLoading)
        assertTrue(state.hasLoaded)
    }

    @Test
    fun `Given recipes are on screen When a refresh fails Then they are not thrown away`() =
        runTest(dispatcher) {
            expiring.items = listOf(item("Mleko", "milk", 2))
            source.result = Result.success(listOf(recipe("Naleśniki")))
            val viewModel = viewModel()
            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            source.result = Result.failure(IllegalStateException("offline"))
            viewModel.onIntent(RecipesIntent.RefreshClicked)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(RecipesFailure.Unavailable, state.failure)
            assertContentEquals(listOf("Naleśniki"), state.recipes.map { it.title })
        }

    @Test
    fun `Given the pantry is read When it happens Then it is capped rather than unbounded`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onIntent(RecipesIntent.Shown("pl"))
            advanceUntilIdle()

            // Prompt length is money, and the cap matches the backend's own so truncation happens
            // here rather than silently there.
            assertEquals(MAX_RECIPE_INGREDIENTS, expiring.lastWindow)
            assertEquals(1, expiring.calls)
        }
}
