package com.appotato.features.recipes.implementation

import com.appotato.shared.recipe.source.api.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeMatchTest {

    private fun recipe(uses: Int, missing: Int) = Recipe(
        title = "x",
        summary = "",
        usesIngredients = List(uses) { "have-$it" },
        missingIngredients = List(missing) { "buy-$it" },
        steps = listOf("Cook."),
        minutes = null
    )

    @Test
    fun `Given nothing to buy When the match is read Then it is a full match`() {
        assertEquals(100, recipe(uses = 3, missing = 0).matchPercent())
    }

    @Test
    fun `Given half the ingredients missing When the match is read Then it is half`() {
        assertEquals(50, recipe(uses = 2, missing = 2).matchPercent())
    }

    @Test
    fun `Given a recipe measuring what you own When it is read Then missing items count against it`() {
        // The number answers "can I cook this now", not "how much of my fridge does it clear" —
        // three owned ingredients score worse here than two, because of the shopping trip.
        assertEquals(60, recipe(uses = 3, missing = 2).matchPercent())
        assertEquals(100, recipe(uses = 2, missing = 0).matchPercent())
    }

    @Test
    fun `Given a recipe listing no ingredients at all When it is read Then it does not divide by zero`() {
        assertEquals(0, recipe(uses = 0, missing = 0).matchPercent())
    }
}
