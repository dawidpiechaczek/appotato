package com.appotato.shared.serialization.implementation

import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.serialization.api.JsonSerializer
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal class JsonSerializerImpl(
    private val json: Json,
    private val coroutineDispatchers: CoroutineDispatchers,
) : JsonSerializer {

    override suspend fun <T> encode(
        serializer: KSerializer<T>,
        value: T
    ): Result<String> = withContext(coroutineDispatchers.default) {
        try {
            Result.success(json.encodeToString(serializer, value))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun <T> decode(
        deserializer: KSerializer<T>,
        jsonString: String
    ): Result<T> = withContext(coroutineDispatchers.default) {
        try {
            Result.success(json.decodeFromString(deserializer, jsonString))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

}
