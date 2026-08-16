package com.appotato.di

import android.app.Application
import com.appotato.shared.remote.config.implementation.remoteConfigModule
import com.appotato.shared.telemetry.implementation.telemetryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun setupKoin(application: Application): KoinApplication = startKoin {
    androidLogger()
    androidContext(application)
    modules(appModules())
}.also { koin -> koin.setupImageLoader() }

internal actual fun platformModules(): List<Module> = listOf(telemetryModule(), remoteConfigModule())
