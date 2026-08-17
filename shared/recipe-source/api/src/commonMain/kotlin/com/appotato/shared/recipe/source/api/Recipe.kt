package com.appotato.shared.recipe.source.api

/**
 * One suggestion, already written in the language the request asked for.
 *
 * Nothing here is an identifier and nothing is meant to be stored: a suggestion is derived from
 * what happens to be in the pantry today, and it stops being true as soon as that changes.
 */
public data class Recipe(
    public val title: String,
    /** A sentence or two — what this is and why it fits what is about to go off. */
    public val summary: String,
    /**
     * Which of the requested items this actually uses, echoed back as the display names that were
     * sent. Echoed rather than re-derived so the UI can highlight the matching pantry rows without
     * having to match strings itself.
     */
    public val usesIngredients: List<String>,
    /** Everything else the recipe needs, so the user can see the shopping cost up front. */
    public val missingIngredients: List<String>,
    public val steps: List<String>,
    /** Rough total time. Null when the source would only be guessing. */
    public val minutes: Int?
)

/**
 * One thing in the pantry, as a recipe source needs to see it.
 *
 * Both the code and the name are sent on purpose. [code] is what makes two spellings of one food
 * comparable and what a cache can be keyed on; [displayName] is the fallback when nothing resolved,
 * and is also what comes back in [Recipe.usesIngredients].
 */
public data class RecipeIngredient(
    /** Stable, language-neutral, from `:shared:ingredients`. Null when nothing could be resolved. */
    public val code: String?,
    public val displayName: String,
    /** Negative for something already past its date — the source may reasonably skip those. */
    public val daysUntilExpiry: Int
)

/**
 * [languageTag] is BCP-47 (`pl`, `en`) and is not optional: the whole point of asking a generative
 * source rather than a fixed recipe database is that the answer comes back in the user's language,
 * even when the pantry names are in a different one.
 */
public data class RecipeRequest(
    public val ingredients: List<RecipeIngredient>,
    public val languageTag: String,
    public val maxRecipes: Int = DEFAULT_MAX_RECIPES
) {
    public companion object {
        /** Enough to choose between, few enough to read without scrolling. */
        public const val DEFAULT_MAX_RECIPES: Int = 3
    }
}
