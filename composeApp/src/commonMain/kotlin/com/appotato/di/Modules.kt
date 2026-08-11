package com.appotato.di

import com.appotato.shared.app.update.implementation.appUpdateModule
import org.koin.core.module.Module

/**
 * Bindings that only exist on one platform — Firebase on Android, the Swift implementations on iOS.
 */
internal expect fun platformModules(): List<Module>

/**
 * [appUpdateModule] is the same on both platforms, but it resolves a `RemoteConfig` that only
 * [platformModules] can provide — Firebase on Android, the Swift binding on iOS.
 */
internal fun appModules(): List<Module> = platformModules() + appUpdateModule()
