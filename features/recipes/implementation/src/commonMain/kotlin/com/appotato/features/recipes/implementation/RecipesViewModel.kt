package com.appotato.features.recipes.implementation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.recipe.source.api.RecipeIngredient
import com.appotato.shared.recipe.source.api.RecipeRequest
import com.appotato.shared.recipe.source.api.RecipeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class RecipesViewModel(
    private val expiringItems: ExpiringItems,
    private val recipeSource: RecipeSource,
    private val today: Today,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _state = MutableStateFlow(RecipesState())
    val state: StateFlow<RecipesState> = _state.asStateFlow()

    /** Remembered from the last [RecipesIntent.Shown] so a refresh does not need it passed again. */
    private var languageTag: String = DEFAULT_LANGUAGE_TAG

    fun onIntent(intent: RecipesIntent) {
        when (intent) {
            is RecipesIntent.Shown -> {
                languageTag = intent.languageTag
                // Every appearance sends this; only the first one costs anything. Suggestions are
                // generated, and switching tabs is not a reason to generate them again.
                if (!_state.value.hasLoaded && !_state.value.isLoading) load()
            }

            RecipesIntent.RefreshClicked -> if (!_state.value.isLoading) load()
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, failure = null) }

        viewModelScope.launch(dispatchers.io) {
            val items = expiringItems.soonestFirst(today(), MAX_RECIPE_INGREDIENTS)
            val urgent = items.count { item -> item.daysUntilExpiry <= RECIPE_TRIGGER_DAYS }

            // Two different questions, and conflating them into one window is what made this
            // screen contradict the pantry. Whether to suggest at all is about urgency — nothing
            // going off soon means nothing to say, and no reason to spend a token. What to build
            // the suggestion *from* is about availability, so that takes the whole pantry: the
            // model is told to build around what expires first, and more to work with is what
            // turns one carton of milk into something cookable.
            if (urgent == 0) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        recipes = emptyList(),
                        basedOn = emptyList(),
                        urgentCount = 0,
                        hasLoaded = true
                    )
                }
                return@launch
            }

            recipeSource.suggestFor(requestFor(items))
                .onSuccess { recipes -> onLoaded(recipes, items, urgent) }
                .onFailure { onFailed(items, urgent) }
        }
    }

    private fun requestFor(items: List<ExpiringItem>) = RecipeRequest(
        ingredients = items.map { item ->
            RecipeIngredient(
                // Sent alongside the name rather than instead of it: the code is what lets the
                // backend cache two spellings of one food together, the name is what comes back in
                // `usesIngredients` for the screen to match on.
                code = item.ingredientCode,
                displayName = item.name,
                daysUntilExpiry = item.daysUntilExpiry
            )
        },
        languageTag = languageTag
    )

    private fun onLoaded(recipes: List<Recipe>, items: List<ExpiringItem>, urgent: Int) {
        _state.update {
            it.copy(
                isLoading = false,
                recipes = recipes,
                basedOn = items.map(ExpiringItem::name),
                urgentCount = urgent,
                failure = null,
                hasLoaded = true
            )
        }
    }

    /**
     * Whatever was on screen stays there. A failed refresh should not throw away suggestions the
     * user was reading, so the failure is additive rather than replacing the content.
     */
    private fun onFailed(items: List<ExpiringItem>, urgent: Int) {
        _state.update {
            it.copy(
                isLoading = false,
                basedOn = items.map(ExpiringItem::name),
                urgentCount = urgent,
                failure = RecipesFailure.Unavailable,
                hasLoaded = true
            )
        }
    }

    private companion object {
        /** Only reached if the screen somehow asks before it has told us the locale. */
        const val DEFAULT_LANGUAGE_TAG = "en"
    }
}
