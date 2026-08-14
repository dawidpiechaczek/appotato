package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.data.RoomPantryRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Binds the pantry. It resolves a `PantryItemDao`, so `databaseModule()` has to be loaded first.
 */
public fun pantryModule(): Module = module {
    single<PantryRepository> { RoomPantryRepository(get()) }
    factory<Today> { SystemToday() }
    viewModelOf(::PantryViewModel)
}
