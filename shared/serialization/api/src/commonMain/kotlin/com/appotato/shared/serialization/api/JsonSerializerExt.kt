package com.appotato.shared.serialization.api

import kotlinx.serialization.serializer

public suspend inline fun <reified T> JsonSerializer.encode(value: T): Result<String> =
    encode(serializer(), value)

public suspend inline fun <reified T> JsonSerializer.decode(jsonString: String): Result<T> =
    decode(serializer(), jsonString)
