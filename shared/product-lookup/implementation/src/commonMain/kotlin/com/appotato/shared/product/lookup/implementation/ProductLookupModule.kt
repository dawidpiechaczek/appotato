package com.appotato.shared.product.lookup.implementation

import com.appotato.shared.product.lookup.api.ProductLookup
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds the lookup. It resolves the shared `HttpClient`, so `networkModule()` has to be loaded too.
 */
public fun productLookupModule(): Module = module {
    single<ProductLookup> { OpenFoodFactsProductLookup(get()) }
}
