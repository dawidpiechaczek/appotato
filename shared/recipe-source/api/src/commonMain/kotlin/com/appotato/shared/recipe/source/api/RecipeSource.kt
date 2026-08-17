package com.appotato.shared.recipe.source.api

/**
 * Turns what is about to go off into something to cook.
 *
 * The contract names no vendor, no model and no backend — which one answers is a decision that
 * lives in the implementation module, and swapping it must not reach any caller. That is the same
 * boundary `ProductLookup` draws around Open Food Facts, and it is what makes a locally-shipped
 * recipe pack and a hosted generative source interchangeable.
 */
public interface RecipeSource {

    /**
     * An empty success is a real answer: the pantry held nothing worth building a recipe out of.
     * A `Result.failure` means the question could not be asked — no connection, a timeout, a
     * refused request — and is the only case a caller should offer to retry.
     *
     * Suggestions are derived, never authoritative. A caller may show them, cache them for the
     * session, and throw them away the moment the pantry changes.
     */
    public suspend fun suggestFor(request: RecipeRequest): Result<List<Recipe>>
}
