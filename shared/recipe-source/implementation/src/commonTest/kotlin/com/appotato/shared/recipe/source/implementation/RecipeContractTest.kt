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
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The other half of this contract is `functions/src/` — a separate language with its own copy of
 * these field names, and nothing but this file standing between a rename on one side and empty
 * recipe cards on the other.
 *
 * The fixtures below are the single source of truth for the wire format. `npm run contract:check`
 * in `functions/` reads *this file*, pulls the JSON back out, and checks it against the schema the
 * function actually constrains the model with and against the parser the function actually runs.
 * So a rename here fails that check, and a rename there fails it too.
 *
 * Why this side owns the fixtures: `commonTest` runs on iOS as well as the JVM and has no portable
 * way to read a file, so a shared `.json` on disk could not be the source. Node can read Kotlin;
 * Kotlin/Native cannot read arbitrary files.
 *
 * Everything runs through the shipping `HttpClient`, because the serialisation settings *are* the
 * contract: `ignoreUnknownKeys` and `coerceInputValues` mean a renamed field never throws, it
 * quietly becomes a default. So the assertions are about values landing, not about parsing.
 */
class RecipeContractTest {

    private val requests = mutableListOf<HttpRequestData>()

    /** Only ever used to read back bytes the shipping client already produced. */
    private val inspector = Json

    private class EndpointStub : RemoteConfig {
        override fun refresh(onResult: (Boolean) -> Unit) = onResult(true)
        override fun getString(key: String): String = "https://example.invalid/suggestRecipes"
        override fun getBoolean(key: String): Boolean = false
        override fun getLong(key: String): Long = 0L
    }

    private class TokenStub : AttestationTokens {
        override fun token(onResult: (String?) -> Unit) = onResult("attest-123")
    }

    private fun source(body: String) = ProxiedRecipeSource(
        client = appotatoHttpClient(
            engine = MockEngine { request ->
                requests += request
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            },
            maxRetries = 0
        ),
        remoteConfig = EndpointStub(),
        attestation = TokenStub()
    )

    private fun sentBody(): Map<String, kotlinx.serialization.json.JsonElement> =
        inspector.parseToJsonElement((requests.single().body as TextContent).text).jsonObject

    @Test
    fun `Given the agreed response When it is read Then every field lands somewhere`() = runTest {
        val recipes = source(RESPONSE_FIXTURE).suggestFor(anyRequest()).getOrThrow()

        assertEquals(1, recipes.size, "the fixture recipe was dropped as hollow")
        val recipe = recipes.single()
        // Each of these is a field name shared with the backend. A rename on either side collapses
        // exactly one of them to its default, so they are asserted one by one rather than as a
        // whole-object equality that would report only the first difference.
        assertEquals("Naleśniki na mleku", recipe.title)
        assertEquals("Zużywa mleko, które kończy się za dwa dni.", recipe.summary)
        assertEquals(listOf("Mleko UHT 3,2%"), recipe.usesIngredients)
        assertEquals(listOf("mąka", "jajka"), recipe.missingIngredients)
        assertEquals(listOf("Wymieszaj składniki.", "Smaż na patelni."), recipe.steps)
        assertEquals(20, recipe.minutes)
    }

    @Test
    fun `Given a request When it goes out Then it is the shape the backend parses`() = runTest {
        source(EMPTY_RESPONSE).suggestFor(
            RecipeRequest(
                ingredients = listOf(
                    RecipeIngredient(code = "milk", displayName = "Mleko UHT 3,2%", daysUntilExpiry = 2)
                ),
                languageTag = "pl",
                maxRecipes = 3
            )
        )

        val body = sentBody()
        assertEquals(REQUEST_KEYS, body.keys)
        assertEquals("\"pl\"", body["languageTag"].toString())
        assertEquals("3", body["maxRecipes"].toString())

        val ingredient = body["ingredients"]!!.jsonArray.single().jsonObject
        assertEquals(INGREDIENT_KEYS, ingredient.keys)
        assertEquals("\"milk\"", ingredient["code"].toString())
        assertEquals("\"Mleko UHT 3,2%\"", ingredient["displayName"].toString())
        assertEquals("2", ingredient["daysUntilExpiry"].toString())
    }

    @Test
    fun `Given an unresolved ingredient When it goes out Then the code is absent not null`() = runTest {
        // `explicitNulls = false` drops a null code from the body entirely, so the backend has to
        // treat a missing `code` the same as an explicit null — and it does. Asserting the shape
        // here is what makes a change to the client's Json config fail on this side rather than
        // silently in production.
        source(EMPTY_RESPONSE).suggestFor(
            RecipeRequest(
                ingredients = listOf(RecipeIngredient(null, "Zestaw upominkowy", 5)),
                languageTag = "en"
            )
        )

        val ingredient = sentBody()["ingredients"]!!.jsonArray.single().jsonObject
        assertTrue("code" !in ingredient.keys, "a null code was serialised; the backend expects it absent")
        assertTrue("displayName" in ingredient.keys)
    }

    @Test
    fun `Given the response fixture When read as a tree Then it names exactly the agreed keys`() {
        // Guards the fixture itself: `contract:check` compares these same names against the
        // function's JSON schema, so if the fixture drifts the cross-language check drifts with it.
        val tree = inspector.parseToJsonElement(RESPONSE_FIXTURE).jsonObject
        assertEquals(setOf("recipes"), tree.keys)
        assertEquals(RECIPE_KEYS, tree["recipes"]!!.jsonArray.single().jsonObject.keys)
    }

    private fun anyRequest() = RecipeRequest(
        ingredients = listOf(RecipeIngredient("milk", "Mleko UHT 3,2%", 2)),
        languageTag = "pl"
    )

    private companion object {
        val REQUEST_KEYS = setOf("ingredients", "languageTag", "maxRecipes")
        val INGREDIENT_KEYS = setOf("code", "displayName", "daysUntilExpiry")
        val RECIPE_KEYS = setOf(
            "title", "summary", "usesIngredients", "missingIngredients", "steps", "minutes"
        )

        const val EMPTY_RESPONSE = """{"recipes":[]}"""

        /**
         * Read by `functions/src/contract-check.ts` — keep it one triple-quoted literal assigned to
         * this exact name, because the check extracts it by looking for precisely that.
         */
        const val RESPONSE_FIXTURE: String = """
{
  "recipes": [
    {
      "title": "Naleśniki na mleku",
      "summary": "Zużywa mleko, które kończy się za dwa dni.",
      "usesIngredients": ["Mleko UHT 3,2%"],
      "missingIngredients": ["mąka", "jajka"],
      "steps": ["Wymieszaj składniki.", "Smaż na patelni."],
      "minutes": 20
    }
  ]
}
"""

        /** The mirror of the above for the outbound direction. Same extraction rules. */
        const val REQUEST_FIXTURE: String = """
{
  "ingredients": [
    { "code": "milk", "displayName": "Mleko UHT 3,2%", "daysUntilExpiry": 2 },
    { "displayName": "Zestaw upominkowy", "daysUntilExpiry": -1 }
  ],
  "languageTag": "pl",
  "maxRecipes": 3
}
"""
    }
}
