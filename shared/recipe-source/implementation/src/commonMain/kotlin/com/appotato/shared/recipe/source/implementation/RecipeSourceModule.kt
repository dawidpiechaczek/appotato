package com.appotato.shared.recipe.source.implementation

import com.appotato.shared.recipe.source.api.RecipeSource
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the recipe source. It resolves the shared `HttpClient` from `networkModule()`, and both a
 * `RemoteConfig` and an `AttestationTokens` that only the platform modules can provide — Firebase
 * on Android, the Swift bindings on iOS.
 */
public fun recipeSourceModule(): Module = module {
    single<RecipeSource> { ProxiedRecipeSource(get(), get(), get()) }
}
