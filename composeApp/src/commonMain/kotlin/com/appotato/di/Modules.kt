package com.appotato.di

import com.appotato.features.pantry.implementation.pantryModule
import com.appotato.features.paywall.implementation.paywallModule
import com.appotato.shared.app.update.implementation.appUpdateModule
import com.appotato.shared.billing.implementation.billingModule
import com.appotato.shared.database.databaseModule
import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.network.networkModule
import com.appotato.shared.product.lookup.implementation.productLookupModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Bindings that only exist on one platform — Firebase on Android, the Swift implementations on iOS.
 */
internal expect fun platformModules(): List<Module>

/**
 * Order matters only in that [pantryModule] resolves a DAO from [databaseModule] and a
 * `ProductLookup` from [productLookupModule], which in turn resolves the `HttpClient` from
 * [networkModule]; [appUpdateModule] resolves a `RemoteConfig` that only [platformModules] can
 * provide. Koin itself is lazy, so the list order is documentation rather than a constraint.
 */
internal fun appModules(): List<Module> = platformModules() + listOf(
    coreModule(),
    networkModule(),
    databaseModule(),
    appUpdateModule(),
    billingModule(),
    productLookupModule(),
    pantryModule(),
    paywallModule()
)

/**
 * Small shared types that no module owns. [CoroutineDispatchers] is injected rather than read off
 * `Dispatchers` so ViewModels stay testable under `runTest`.
 */
private fun coreModule(): Module = module {
    single { CoroutineDispatchers() }
}
