package com.appotato.shared.product.lookup.implementation

import com.appotato.shared.network.appotatoHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs against the shipping `HttpClient` over `MockEngine`, so the JSON configuration under test is
 * the one the app uses. Retries are off — the production backoff is seconds long and the failure
 * paths here would spend all of it waiting.
 */
class OpenFoodFactsProductLookupTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun lookupReturning(status: HttpStatusCode, body: String) = lookup { _ ->
        respond(content = body, status = status, headers = headersOf("Content-Type", "application/json"))
    }

    private fun lookupFailingWith(cause: Throwable) = lookup { throw cause }

    private fun lookup(respond: MockEngineHandler) = OpenFoodFactsProductLookup(
        appotatoHttpClient(
            engine = MockEngine { request ->
                requests += request
                respond(request)
            },
            maxRetries = 0
        )
    )

    @Test
    fun `Given a catalogued product When it is looked up Then its name and calories come back`() = runTest {
        val product = lookupReturning(HttpStatusCode.OK, NUTELLA_JSON)
            .byBarcode("3017620422003")
            .getOrThrow()

        assertEquals("3017620422003", product?.barcode)
        assertEquals("Ferrero Nutella", product?.name)
        assertEquals("400 g", product?.quantity)
        assertEquals(539, product?.nutrition?.caloriesPer100g)
        assertEquals(listOf("en:spreads", "en:sweet-spreads"), product?.categoryTags)
        assertEquals("https://images.openfoodfacts.org/front_en.200.jpg", product?.imageUrl)
    }

    @Test
    fun `Given a lookup When the request is built Then only the mapped fields are asked for`() = runTest {
        lookupReturning(HttpStatusCode.OK, NUTELLA_JSON).byBarcode("3017620422003")

        val url = requests.single().url
        assertTrue(url.encodedPath.endsWith("/api/v2/product/3017620422003.json"), "was $url")
        assertEquals(
            "product_name,brands,quantity,image_url,image_small_url,categories_tags,serving_size,nutriments",
            url.parameters["fields"]
        )
    }

    @Test
    fun `Given an unknown barcode When it is looked up Then the miss is a success with no product`() = runTest {
        val result = lookupReturning(HttpStatusCode.NotFound, NOT_FOUND_JSON).byBarcode("5901234123457")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `Given a code the source rejects When it is looked up Then the empty record is a miss`() = runTest {
        val result = lookupReturning(HttpStatusCode.OK, NOT_FOUND_JSON).byBarcode("5901234123457")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `Given a QR code rather than an EAN When it is looked up Then nothing is requested`() = runTest {
        val result = lookupReturning(HttpStatusCode.OK, NUTELLA_JSON).byBarcode("https://example.com")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `Given an empty barcode When it is looked up Then nothing is requested`() = runTest {
        assertNull(lookupReturning(HttpStatusCode.OK, NUTELLA_JSON).byBarcode("").getOrThrow())

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `Given the source is broken When a product is looked up Then the failure is reported`() = runTest {
        val result = lookupReturning(HttpStatusCode.BadGateway, "").byBarcode("3017620422003")

        assertTrue(result.isFailure)
    }

    @Test
    fun `Given no connection When a product is looked up Then the failure is reported`() = runTest {
        val result = lookupFailingWith(IOException("offline")).byBarcode("3017620422003")

        assertTrue(result.isFailure)
    }

    @Test
    fun `Given a response that is not JSON When a product is looked up Then the failure is reported`() = runTest {
        val result = lookupReturning(HttpStatusCode.OK, "<html>captive portal</html>").byBarcode("3017620422003")

        assertTrue(result.isFailure)
    }

    private companion object {
        const val NOT_FOUND_JSON = """{"code":"5901234123457","status":0,"status_verbose":"product not found"}"""

        // Trimmed to the requested fields, otherwise verbatim from the live endpoint — including the
        // calories arriving as a decimal, a mixed-case tag, and a nutriment the app does not map.
        const val NUTELLA_JSON = """
            {
              "code": "3017620422003",
              "status": 1,
              "product": {
                "product_name": "Nutella",
                "brands": "Ferrero, Nutella",
                "quantity": "400 g",
                "image_url": "https://images.openfoodfacts.org/front_en.400.jpg",
                "image_small_url": "https://images.openfoodfacts.org/front_en.200.jpg",
                "categories_tags": ["en:Spreads", "en:sweet-spreads"],
                "nutriments": {
                  "energy-kcal_100g": 539.0,
                  "energy-kj_100g": 2252.0
                }
              }
            }
        """
    }
}

/** What `MockEngine` hands its handler: a request in, a canned response out. */
private typealias MockEngineHandler =
    suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) ->
    io.ktor.client.request.HttpResponseData
