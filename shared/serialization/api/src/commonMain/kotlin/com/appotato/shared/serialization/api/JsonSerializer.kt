package com.appotato.shared.serialization.api

import kotlinx.serialization.KSerializer

public interface JsonSerializer {
    public suspend fun <T> encode(serializer: KSerializer<T>, value: T): Result<String>
    public suspend fun <T> decode(deserializer: KSerializer<T>, jsonString: String): Result<T>
}
