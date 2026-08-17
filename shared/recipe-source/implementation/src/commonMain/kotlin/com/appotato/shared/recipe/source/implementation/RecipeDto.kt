package com.appotato.shared.recipe.source.implementation

import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.recipe.source.api.RecipeIngredient
import com.appotato.shared.recipe.source.api.RecipeRequest
import kotlinx.serialization.Serializable

@Serializable
internal data class SuggestRequestDto(
    val ingredients: List<IngredientDto>,
    val languageTag: String,
    val maxRecipes: Int
)

@Serializable
internal data class IngredientDto(
    val code: String?,
    val displayName: String,
    val daysUntilExpiry: Int
)

/**
 * Every field carries a default. The body is our own backend's and so is less of a moving target
 * than a third party's, but it is still a document arriving over a network — a response missing a
 * field should cost one malformed recipe, not the whole answer.
 */
@Serializable
internal data class SuggestResponseDto(
    val recipes: List<RecipeDto> = emptyList()
)

@Serializable
internal data class RecipeDto(
    val title: String = "",
    val summary: String = "",
    val usesIngredients: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val minutes: Int? = null
)

internal fun RecipeRequest.toDto(): SuggestRequestDto = SuggestRequestDto(
    ingredients = ingredients.map(RecipeIngredient::toDto),
    languageTag = languageTag,
    maxRecipes = maxRecipes
)

internal fun RecipeIngredient.toDto(): IngredientDto = IngredientDto(
    code = code,
    displayName = displayName,
    daysUntilExpiry = daysUntilExpiry
)

internal fun RecipeDto.toDomain(): Recipe = Recipe(
    title = title,
    summary = summary,
    usesIngredients = usesIngredients,
    missingIngredients = missingIngredients,
    steps = steps,
    minutes = minutes
)

/**
 * A recipe with no title or no steps is not one — it is a hole in the response. Dropping it here
 * keeps the check in one place instead of in every screen that renders a suggestion.
 */
internal fun SuggestResponseDto.toDomain(): List<Recipe> = recipes
    .filter { recipe -> recipe.title.isNotBlank() && recipe.steps.isNotEmpty() }
    .map(RecipeDto::toDomain)
