package com.appotato.features.recipes.implementation

internal sealed interface RecipesIntent {

    /**
     * The tab became visible. Carries the language because the device locale is a Compose concern
     * and reading it here would drag `androidx.compose.ui` into the ViewModel.
     *
     * Sent on every appearance; the ViewModel decides whether that means a request.
     */
    data class Shown(val languageTag: String) : RecipesIntent

    /** The user explicitly asked for new suggestions, so a cached result is not good enough. */
    data object RefreshClicked : RecipesIntent
}
