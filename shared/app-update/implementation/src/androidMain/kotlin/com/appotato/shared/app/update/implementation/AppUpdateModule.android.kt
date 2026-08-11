package com.appotato.shared.app.update.implementation

import android.content.Context
import com.appotato.shared.app.update.api.AppUpdateChecker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

public actual fun appUpdateModule(): Module = module {
    single<AppUpdateChecker> {
        RemoteConfigAppUpdateChecker(
            remoteConfig = get(),
            installedVersionName = androidContext().versionName()
        )
    }
}

// Deprecated in API 33 but works on every level; the PackageInfoFlags replacement starts at 33, so
// with minSdk 26 using it would mean a version branch doing the same thing on both sides.
@Suppress("DEPRECATION")
private fun Context.versionName(): String = runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName
}.getOrNull().orEmpty()
