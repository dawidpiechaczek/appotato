package com.appotato.shared.serialization.fake

import com.appotato.shared.serialization.api.JsonSerializer
import kotlinx.serialization.KSerializer

public class JsonSerializerFake<T> : JsonSerializer {

    public var encodedJsonResult = Result.success("")
    public var decodedJsonResult = Result.success("")

    override suspend fun <T> encode(
        serializer: KSerializer<T>,
        value: T
    ): Result<String> = encodedJsonResult


    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> decode(
        deserializer: KSerializer<T>,
        jsonString: String
    ): Result<T> = decodedJsonResult as Result<T>

}
