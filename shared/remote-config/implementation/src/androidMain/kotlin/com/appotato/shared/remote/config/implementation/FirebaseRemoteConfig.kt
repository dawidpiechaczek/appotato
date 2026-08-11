package com.appotato.shared.remote.config.implementation

import com.appotato.shared.remote.config.api.RemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig as FirebaseRemoteConfigSdk

internal class FirebaseRemoteConfig(
    private val config: FirebaseRemoteConfigSdk
) : RemoteConfig {

    override fun refresh(onResult: (Boolean) -> Unit) {
        config.fetchAndActivate().addOnCompleteListener { task -> onResult(task.isSuccessful) }
    }

    override fun getString(key: String): String = config.getString(key)

    override fun getBoolean(key: String): Boolean = config.getBoolean(key)

    override fun getLong(key: String): Long = config.getLong(key)
}
