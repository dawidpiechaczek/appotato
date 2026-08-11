package com.appotato.shared.app.update.implementation

import org.koin.core.module.Module

/**
 * Binds [com.appotato.shared.app.update.api.AppUpdateChecker]. Expect/actual because reading the
 * installed version name is platform work — `PackageManager` on Android, the main bundle on iOS —
 * and requires a [com.appotato.shared.remote.config.api.RemoteConfig] binding to already be in the
 * graph.
 */
public expect fun appUpdateModule(): Module
