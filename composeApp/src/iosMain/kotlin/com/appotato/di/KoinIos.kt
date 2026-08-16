package com.appotato.di

import com.appotato.shared.remote.config.api.RemoteConfig
import com.appotato.shared.telemetry.api.Telemetry
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Called from iOSApp.swift with the Swift-side [Telemetry] and [RemoteConfig] implementations.
 *
 * The Firebase iOS SDK is Swift/ObjC and lives in the Xcode project, so those bindings are written
 * there and injected here instead of being bridged into Kotlin/Native through cinterop. That keeps
 * GoogleService-Info.plist, the SPM packages and the dSYM upload phase in the one place they have
 * to live anyway.
 */
fun setupKoin(
    telemetry: Telemetry,
    remoteConfig: RemoteConfig
): KoinApplication = startKoin {
    modules(
        appModules() + module {
            single<Telemetry> { telemetry }
            single<RemoteConfig> { remoteConfig }
        }
    )
}.also { koin -> koin.setupImageLoader() }

internal actual fun platformModules(): List<Module> = emptyList()
