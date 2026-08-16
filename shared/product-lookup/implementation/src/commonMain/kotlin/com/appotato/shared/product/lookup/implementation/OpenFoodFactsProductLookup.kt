package com.appotato.shared.product.lookup.implementation

import com.appotato.shared.product.lookup.api.Product
import com.appotato.shared.product.lookup.api.ProductLookup
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Open Food Facts is the only place this vendor is named. It is an open database with no key and no
 * quota worth planning around, which is what makes it usable from the client at all — the app has
 * no backend to proxy through.
 *
 * Swapping it for another source means writing another [ProductLookup] and changing one line of
 * [productLookupModule]; nothing above this file knows where a product came from.
 */
internal class OpenFoodFactsProductLookup(private val client: HttpClient) : ProductLookup {

    override suspend fun byBarcode(barcode: String): Result<Product?> {
        // The code goes into the path, and the scanner will happily hand over the contents of a QR
        // code. Anything that is not a plain EAN/UPC is not in this database anyway.
        if (barcode.isEmpty() || !barcode.all { character -> character.isDigit() }) {
            return Result.success(null)
        }

        return try {
            val response = client.get("$PRODUCT_URL$barcode.json") {
                // Without this the response is the entire record — around 100 kB of ingredient
                // analysis and translations for a page that shows a name and a calorie count.
                parameter("fields", REQUESTED_FIELDS)
            }

            when {
                // The documented answer for an unknown code, and an ordinary one.
                response.status == HttpStatusCode.NotFound -> Result.success(null)
                response.status.isSuccess() ->
                    Result.success(response.body<ProductResponseDto>().product?.toDomain(barcode))
                else -> Result.failure(IOException("Product lookup failed with ${response.status}"))
            }
        } catch (exception: IOException) {
            // Covers timeouts too: Ktor's timeout exception is one of these.
            Result.failure(exception)
        } catch (exception: ContentConvertException) {
            // JSON that does not match the DTOs, or a body that is not JSON at all behind a JSON
            // content type — a captive portal's login page is the usual reason.
            Result.failure(exception)
        } catch (exception: SerializationException) {
            Result.failure(exception)
        } catch (exception: NoTransformationFoundException) {
            // A success code carrying a content type nothing here can read.
            Result.failure(exception)
        }
    }

    private companion object {
        const val PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/"

        /** Everything the app maps, and nothing else. Order matches `ProductDto`. */
        const val REQUESTED_FIELDS =
            "product_name,brands,quantity,image_url,image_small_url,categories_tags,serving_size,nutriments"
    }
}
