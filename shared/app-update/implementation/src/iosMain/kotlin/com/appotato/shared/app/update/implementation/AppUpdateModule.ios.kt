package com.appotato.shared.app.update.implementation

import com.appotato.shared.app.update.api.AppUpdateChecker
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

private const val SHORT_VERSION_KEY = "CFBundleShortVersionString"

public actual fun appUpdateModule(): Module = module {
    single<AppUpdateChecker> {
        RemoteConfigAppUpdateChecker(
            remoteConfig = get(),
            // CFBundleShortVersionString, not CFBundleVersion: the former is the marketing version
            // that matches what Android reports as versionName.
            installedVersionName = NSBundle.mainBundle.objectForInfoDictionaryKey(SHORT_VERSION_KEY) as? String ?: ""
        )
    }
}
