package com.appotato.features.recipes.implementation

import com.appotato.features.recipes.implementation.data.RoomExpiringItems
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Binds the recipes tab. It resolves a `PantryItemDao` from `databaseModule()` and a `RecipeSource`
 * from `recipeSourceModule()`, so both have to be loaded.
 *
 * There is no `Billing` here on purpose. Suggestions are open to everyone for now; putting a gate
 * in later is a change to this module and the ViewModel, and to nothing else.
 */
public fun recipesModule(): Module = module {
    single<ExpiringItems> { RoomExpiringItems(get()) }
    factory<Today> { SystemToday() }
    viewModelOf(::RecipesViewModel)
}
