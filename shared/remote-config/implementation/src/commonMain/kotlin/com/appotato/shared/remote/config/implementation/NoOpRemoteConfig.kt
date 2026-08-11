package com.appotato.shared.remote.config.implementation

import com.appotato.shared.remote.config.api.RemoteConfig

/**
 * Fallback used when the platform SDK is not configured. Every value falls back to the zero value
 * of its type, which is what callers already have to handle for a key that was never published.
 */
internal class NoOpRemoteConfig : RemoteConfig {
    override fun refresh(onResult: (Boolean) -> Unit): Unit = onResult(false)
    override fun getString(key: String): String = ""
    override fun getBoolean(key: String): Boolean = false
    override fun getLong(key: String): Long = 0L
}
