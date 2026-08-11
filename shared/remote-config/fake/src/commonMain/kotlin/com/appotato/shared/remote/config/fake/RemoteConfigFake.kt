package com.appotato.shared.remote.config.fake

import com.appotato.shared.remote.config.api.RemoteConfig

/**
 * In-memory [RemoteConfig]. Seed values with [set], then assert on [refreshCount] to check the
 * caller actually asked for fresh values.
 */
public class RemoteConfigFake : RemoteConfig {

    private val values = mutableMapOf<String, String>()

    /** What [refresh] reports back — flip it to false to exercise the offline path. */
    public var refreshSucceeds: Boolean = true

    public var refreshCount: Int = 0
        private set

    /** Values published only once [refresh] runs, mirroring fetch-then-activate. */
    private val pending = mutableMapOf<String, String>()

    public fun set(key: String, value: String) {
        values[key] = value
    }

    public fun set(key: String, value: Boolean) {
        set(key, value.toString())
    }

    public fun set(key: String, value: Long) {
        set(key, value.toString())
    }

    /** Queues a value that only becomes readable after the next successful [refresh]. */
    public fun setOnRefresh(key: String, value: String) {
        pending[key] = value
    }

    public fun clear() {
        values.clear()
        pending.clear()
        refreshCount = 0
        refreshSucceeds = true
    }

    override fun refresh(onResult: (Boolean) -> Unit) {
        refreshCount++
        if (refreshSucceeds) {
            values.putAll(pending)
            pending.clear()
        }
        onResult(refreshSucceeds)
    }

    override fun getString(key: String): String = values[key].orEmpty()

    override fun getBoolean(key: String): Boolean = values[key]?.toBooleanStrictOrNull() ?: false

    override fun getLong(key: String): Long = values[key]?.toLongOrNull() ?: 0L
}
