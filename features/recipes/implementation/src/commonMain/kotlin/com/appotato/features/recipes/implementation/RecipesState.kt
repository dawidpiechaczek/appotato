package com.appotato.features.recipes.implementation

import androidx.compose.runtime.Immutable
import com.appotato.shared.recipe.source.api.Recipe

/**
 * Why a screen has no suggestions to show. Split from "empty" on purpose: an empty pantry window is
 * an answer and offering a retry for it would be nonsense, while a failed request is worth retrying.
 */
internal enum class RecipesFailure {
    /** No connection, a timeout, or the backend refused. Retrying is reasonable. */
    Unavailable
}

@Immutable
internal data class RecipesState(
    val isLoading: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    /** Everything sent to the source — the whole pantry, not just the urgent part. */
    val basedOn: List<String> = emptyList(),
    /**
     * How many of those are going off within [RECIPE_TRIGGER_DAYS]. This is the number worth
     * showing: it is the reason the screen has anything on it, and it is the same count the pantry
     * badges, so the two tabs cannot tell the user different things.
     */
    val urgentCount: Int = 0,
    val failure: RecipesFailure? = null,
    /**
     * Whether a load has finished at least once. Guards against re-asking on every tab switch:
     * suggestions cost tokens, and nothing about them changes just because the user looked away.
     */
    val hasLoaded: Boolean = false
) {
    /** Nothing is urgent, so there is nothing to suggest — not a failure, and not worth a retry. */
    val isEmptyPantry: Boolean = hasLoaded && failure == null && urgentCount == 0

    /** The backend answered, about real items, and had nothing to offer. Rare but possible. */
    val isEmptyResult: Boolean =
        hasLoaded && failure == null && urgentCount > 0 && recipes.isEmpty()
}
