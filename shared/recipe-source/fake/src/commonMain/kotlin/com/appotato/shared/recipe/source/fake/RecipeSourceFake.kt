package com.appotato.shared.recipe.source.fake

import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.recipe.source.api.RecipeRequest
import com.appotato.shared.recipe.source.api.RecipeSource

/**
 * An in-memory [RecipeSource]. [result] is what the next call returns, so a test can set up a hit,
 * an empty answer (`Result.success(emptyList())`) or a failure without touching the network.
 */
public class RecipeSourceFake(
    public var result: Result<List<Recipe>> = Result.success(emptyList())
) : RecipeSource {

    /**
     * Every request made, in order. Enough to assert that a screen asked once rather than once per
     * recomposition, and to inspect what it actually sent — the language especially.
     */
    public val requests: MutableList<RecipeRequest> = mutableListOf()

    override suspend fun suggestFor(request: RecipeRequest): Result<List<Recipe>> {
        requests += request
        return result
    }
}
