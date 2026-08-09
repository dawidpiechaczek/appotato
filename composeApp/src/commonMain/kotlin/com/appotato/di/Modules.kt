package com.appotato.di

import org.koin.core.module.Module

/**
 * Bindings that only exist on one platform — Firebase on Android, the Swift implementation on iOS.
 */
internal expect fun platformModule(): Module

internal fun appModules(): List<Module> = listOf(platformModule())
