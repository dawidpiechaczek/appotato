package com.appotato.features.paywall.implementation

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Binds the paywall's ViewModel. It resolves a `Billing` and a `CoroutineDispatchers`, so this has
 * to be loaded after the module that provides them.
 */
public fun paywallModule(): Module = module {
    viewModelOf(::PaywallViewModel)
}
