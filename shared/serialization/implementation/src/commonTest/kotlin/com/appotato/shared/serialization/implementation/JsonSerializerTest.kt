package com.appotato.shared.serialization.implementation

import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.serialization.api.JsonSerializer
import com.appotato.shared.serialization.api.decode
import com.appotato.shared.serialization.api.encode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class JsonSerializerTest {

    private lateinit var jsonSerializer: JsonSerializer
    private val coroutineDispatchers: CoroutineDispatchers = CoroutineDispatchers()

    @BeforeTest
    fun setUp() {
        jsonSerializer = JsonSerializerImpl(Json, coroutineDispatchers)
    }

    @Test
    fun `Given example test user When encoded Then return expected json with test user data`() =
        runTest {
            val expectedJson = "{\"name\":\"Alfred Tester\",\"age\":18}"
            val testUser = TestUser(name = "Alfred Tester", age = 18)

            val result = jsonSerializer.encode(testUser)

            assertTrue(result.isSuccess)
            assertEquals(expectedJson, result.getOrNull())
        }

    @Test
    fun `Given example test user json When decoded Then return expected test user object`() =
        runTest {
            val json = "{\"name\":\"Alfred Tester\",\"age\":18}"
            val expectedTestUser = TestUser(name = "Alfred Tester", age = 18)

            val result = jsonSerializer.decode<TestUser>(json)

            assertTrue(result.isSuccess)
            assertEquals(expectedTestUser, result.getOrNull())
        }

    @Test
    fun `Given example invalid json When decoded Then return failure result`() = runTest {
        val expectedJson = "{name:Alfred Tester,age:18}"

        val result = jsonSerializer.decode<String>(expectedJson)

        assertTrue(result.isFailure)
    }

    @Test
    fun `Given example text with special characters When encoded Then return expected json`() =
        runTest {
            val json = """{"name":"Alfred Tester","age":18}"""
            val expectedJson = """"{\"name\":\"Alfred Tester\",\"age\":18}""""

            val result = jsonSerializer.encode(json)

            assertTrue(result.isSuccess)
            assertEquals(expectedJson, result.getOrNull())
        }

    @Serializable
    data class TestUser(
        val name: String,
        val age: Int
    )

}
