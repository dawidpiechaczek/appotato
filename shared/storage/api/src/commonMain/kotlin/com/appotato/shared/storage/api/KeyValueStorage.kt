package com.appotato.shared.storage.api

import kotlinx.serialization.KSerializer

public interface KeyValueStorage {
    public suspend fun <T> get(key: String, serializer: KSerializer<T>): T?
}
