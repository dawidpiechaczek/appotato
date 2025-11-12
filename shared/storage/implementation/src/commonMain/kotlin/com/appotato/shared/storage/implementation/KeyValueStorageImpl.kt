package com.appotato.shared.storage.implementation

import com.appotato.shared.storage.api.KeyValueStorage
import kotlinx.serialization.KSerializer

internal class KeyValueStorageImpl: KeyValueStorage {

    override suspend fun <T> get(key: String, serializer: KSerializer<T>): T? {
        TODO("Not yet implemented")
    }

}