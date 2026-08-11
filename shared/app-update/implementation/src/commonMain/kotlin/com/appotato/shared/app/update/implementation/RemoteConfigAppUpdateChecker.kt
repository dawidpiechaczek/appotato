package com.appotato.shared.app.update.implementation

import com.appotato.shared.app.update.api.AppUpdateChecker
import com.appotato.shared.app.update.api.AppUpdateStatus
import com.appotato.shared.app.update.api.AppVersion
import com.appotato.shared.remote.config.api.RemoteConfig
import com.appotato.shared.remote.config.api.refresh

internal class RemoteConfigAppUpdateChecker(
    private val remoteConfig: RemoteConfig,
    private val installedVersionName: String
) : AppUpdateChecker {

    override val installedVersion: AppVersion? = AppVersion.parse(installedVersionName)

    override suspend fun check(): AppUpdateStatus {
        // The result is deliberately ignored: a failed fetch still leaves the previously activated
        // values in place, and those are a better answer than none.
        remoteConfig.refresh()
        return status()
    }

    private fun status(): AppUpdateStatus {
        val installed = installedVersion ?: return AppUpdateStatus.UpToDate
        val minimum = version(RemoteConfigKeys.MINIMUM_VERSION)
        val latest = version(RemoteConfigKeys.LATEST_VERSION)
        val message = remoteConfig.getString(RemoteConfigKeys.UPDATE_MESSAGE).takeIf(String::isNotBlank)
        val storeUrl = remoteConfig.getString(RemoteConfigKeys.UPDATE_URL).takeIf(String::isNotBlank)

        return when {
            minimum != null && installed < minimum -> AppUpdateStatus.Required(minimum, message, storeUrl)
            latest != null && installed < latest -> AppUpdateStatus.Available(latest, message, storeUrl)
            else -> AppUpdateStatus.UpToDate
        }
    }

    private fun version(key: String): AppVersion? = AppVersion.parse(remoteConfig.getString(key))
}
