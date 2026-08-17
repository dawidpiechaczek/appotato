package com.appotato.shared.recipe.source.implementation

import com.appotato.shared.attestation.api.AttestationTokens
import com.appotato.shared.network.appotatoHttpClient
import com.appotato.shared.recipe.source.api.RecipeIngredient
import com.appotato.shared.recipe.source.api.RecipeRequest
import com.appotato.shared.remote.config.api.RemoteConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs against the shipping `HttpClient` over `MockEngine`, so the JSON configuration under test is
 * the one the app uses. Retries are off — the production backoff is seconds long.
 */
class ProxiedRecipeSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private class RemoteConfigStub(private val endpoint: String) : RemoteConfig {
        override fun refresh(onResult: (Boolean) -> Unit) = onResult(true)
        override fun getString(key: String): String = if (key == "recipes_endpoint") endpoint else ""
        override fun getBoolean(key: String): Boolean = false
        override fun getLong(key: String): Long = 0L
    }

    private class AttestationStub(private val value: String?) : AttestationTokens {
        override fun token(onResult: (String?) -> Unit) = onResult(value)
    }

    private fun source(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = EMPTY_BODY,
        endpoint: String = ENDPOINT,
        attestationToken: String? = "attest-123"
    ) = ProxiedRecipeSource(
        client = appotatoHttpClient(
            engine = MockEngine { request ->
                requests += request
                respond(
                    content = body,
                    status = status,
                    headers = headersOf("Content-Type", "application/json")
                )
            },
            maxRetries = 0
        ),
        remoteConfig = RemoteConfigStub(endpoint),
        attestation = AttestationStub(attestationToken)
    )

    private fun request() = RecipeRequest(
        ingredients = listOf(RecipeIngredient("milk", "Mleko UHT 3,2%", daysUntilExpiry = 2)),
        languageTag = "pl"
    )

    @Test
    fun `Given the backend answers When suggestions are asked for Then they are mapped`() = runTest {
        val recipes = source(body = TWO_RECIPES).suggestFor(request()).getOrThrow()

        assertEquals(listOf("Naleśniki", "Budyń"), recipes.map { it.title })
        assertEquals(listOf("Mleko UHT 3,2%"), recipes.first().usesIngredients)
        assertEquals(20, recipes.first().minutes)
        assertEquals(null, recipes.last().minutes)
    }

    @Test
    fun `Given a request is made When it goes out Then it carries the attestation token`() = runTest {
        source(body = EMPTY_BODY).suggestFor(request())

        assertEquals("attest-123", requests.single().headers["X-Firebase-AppCheck"])
    }

    @Test
    fun `Given no endpoint is published When suggestions are asked for Then nothing is sent`() = runTest {
        val result = source(endpoint = "").suggestFor(request())

        assertTrue(result.isFailure)
        assertTrue(requests.isEmpty(), "a request went out with no endpoint configured")
    }

    @Test
    fun `Given attestation fails When suggestions are asked for Then nothing is sent`() = runTest {
        val result = source(attestationToken = null).suggestFor(request())

        assertTrue(result.isFailure)
        assertTrue(requests.isEmpty(), "a request went out that the server would have rejected")
    }

    @Test
    fun `Given the backend rejects the call When suggestions are asked for Then it is a failure`() = runTest {
        val result = source(status = HttpStatusCode.Unauthorized, body = ERROR_BODY).suggestFor(request())

        assertTrue(result.isFailure)
    }

    @Test
    fun `Given a recipe with no steps When the answer is mapped Then it is dropped`() = runTest {
        val recipes = source(body = ONE_GOOD_ONE_HOLLOW).suggestFor(request()).getOrThrow()

        assertEquals(listOf("Naleśniki"), recipes.map { it.title })
    }

    @Test
    fun `Given a body that is not the agreed shape When it is read Then it is a failure`() = runTest {
        val result = source(body = "not json at all").suggestFor(request())

        assertTrue(result.isFailure)
    }

    @Test
    fun `Given an empty pantry answer When it is read Then it is an empty success`() = runTest {
        val recipes = source(body = EMPTY_BODY).suggestFor(request()).getOrThrow()

        assertTrue(recipes.isEmpty())
    }

    private companion object {
        const val ENDPOINT = "https://europe-central2-appotato-dev.cloudfunctions.net/suggestRecipes"
        const val EMPTY_BODY = """{"recipes":[]}"""
        const val ERROR_BODY = """{"error":"unauthenticated"}"""

        const val TWO_RECIPES = """
            {"recipes":[
              {"title":"Naleśniki","summary":"Szybkie i zużywają mleko.",
               "usesIngredients":["Mleko UHT 3,2%"],"missingIngredients":["mąka","jajka"],
               "steps":["Wymieszaj składniki.","Smaż na patelni."],"minutes":20},
              {"title":"Budyń","summary":"Deser na mleku.",
               "usesIngredients":["Mleko UHT 3,2%"],"missingIngredients":["cukier"],
               "steps":["Zagotuj mleko."],"minutes":null}
            ]}
        """

        const val ONE_GOOD_ONE_HOLLOW = """
            {"recipes":[
              {"title":"Naleśniki","summary":"Szybkie.","usesIngredients":["Mleko UHT 3,2%"],
               "missingIngredients":[],"steps":["Wymieszaj."],"minutes":15},
              {"title":"Coś","summary":"","usesIngredients":[],"missingIngredients":[],
               "steps":[],"minutes":null}
            ]}
        """
    }
}
