package com.appotato.features.pantry.implementation

import com.appotato.features.pantry.implementation.data.RoomPantryRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Binds the pantry. It resolves a `PantryItemDao`, so `databaseModule()` has to be loaded first.
 *
 * [PendingScan] is a `single` on purpose: it is the one piece of state the scanner tab and the
 * pantry tab share, and two instances would silently drop every scan.
 */
public fun pantryModule(): Module = module {
    single<PantryRepository> { RoomPantryRepository(get()) }
    single { PendingScan() }
    factory<Today> { SystemToday() }
    viewModelOf(::PantryViewModel)
    viewModelOf(::ScannerViewModel)
}
