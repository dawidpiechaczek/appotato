package com.appotato.shared.remote.config.implementation

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appotato.shared.remote.config.api.RemoteConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import com.google.firebase.remoteconfig.FirebaseRemoteConfig as FirebaseRemoteConfigSdk

private const val TAG = "RemoteConfig"

/** Firebase throttles anything shorter on release builds anyway. */
private const val FETCH_INTERVAL_SECONDS = 3600L

public fun remoteConfigModule(): Module = module {
    single<RemoteConfig> { createRemoteConfig(androidContext()) }
}

private fun createRemoteConfig(context: Context): RemoteConfig =
    if (FirebaseApp.getApps(context).isEmpty()) {
        Log.w(TAG, "Firebase is not configured — google-services.json is missing. Remote config is disabled.")
        NoOpRemoteConfig()
    } else {
        FirebaseRemoteConfig(
            config = FirebaseRemoteConfigSdk.getInstance().apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        // Debuggable builds fetch on every call, otherwise a console change takes an
                        // hour to show up while you are testing it.
                        .setMinimumFetchIntervalInSeconds(
                            if (context.isDebuggable()) 0L else FETCH_INTERVAL_SECONDS
                        )
                        .build()
                )
            }
        )
    }

private fun Context.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
