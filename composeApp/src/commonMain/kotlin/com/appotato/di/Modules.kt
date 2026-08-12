package com.appotato.di

import com.appotato.features.paywall.implementation.paywallModule
import com.appotato.shared.app.update.implementation.appUpdateModule
import com.appotato.shared.billing.implementation.billingModule
import com.appotato.shared.dispatchers.CoroutineDispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Bindings that only exist on one platform — Firebase on Android, the Swift implementations on iOS.
 */
internal expect fun platformModules(): List<Module>

/**
 * [appUpdateModule] is the same on both platforms, but it resolves a `RemoteConfig` that only
 * [platformModules] can provide — Firebase on Android, the Swift binding on iOS.
 *
 * [billingModule] needs nothing platform-specific: a billing SDK that covers both stores from
 * common code is the reason to use one at all.
 */
internal fun appModules(): List<Module> =
    platformModules() + coreModule() + appUpdateModule() + billingModule() + paywallModule()

/**
 * Small shared types that no module owns. [CoroutineDispatchers] is injected rather than read off
 * `Dispatchers` so ViewModels stay testable under `runTest`.
 */
private fun coreModule(): Module = module {
    single { CoroutineDispatchers() }
}
