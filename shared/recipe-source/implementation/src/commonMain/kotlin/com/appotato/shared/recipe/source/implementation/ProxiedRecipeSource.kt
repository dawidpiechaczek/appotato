package com.appotato.shared.recipe.source.implementation

import com.appotato.shared.attestation.api.AttestationTokens
import com.appotato.shared.attestation.api.token
import com.appotato.shared.recipe.source.api.Recipe
import com.appotato.shared.recipe.source.api.RecipeRequest
import com.appotato.shared.recipe.source.api.RecipeSource
import com.appotato.shared.remote.config.api.RemoteConfig
import com.appotato.shared.remote.config.api.refresh
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Asks our own backend, which is the only party that holds the key to whatever generates the
 * recipes. Nothing about the model, the vendor or the prompt appears on this side of the wire —
 * swapping any of them is a deploy, not an app release.
 *
 * The endpoint is not compiled in. It differs per environment, and the app has no other mechanism
 * that already knows which environment it is running in, so it is read from remote config — which
 * is per Firebase project and therefore per environment for free.
 */
internal class ProxiedRecipeSource(
    private val client: HttpClient,
    private val remoteConfig: RemoteConfig,
    private val attestation: AttestationTokens
) : RecipeSource {

    override suspend fun suggestFor(request: RecipeRequest): Result<List<Recipe>> {
        val endpoint = endpoint()

        // Not asked for when there is nowhere to send it — attesting costs a Play Integrity round
        // trip, and a token this call will not use is one the device paid for and threw away.
        val attestationToken = if (endpoint.isBlank()) null else attestation.token()

        return when {
            endpoint.isBlank() ->
                Result.failure(IllegalStateException("No recipes endpoint configured"))
            // The server would reject the request anyway; being told so costs a round trip.
            attestationToken == null ->
                Result.failure(IllegalStateException("No attestation token"))
            else -> post(endpoint, attestationToken, request)
        }
    }

    /**
     * An unfetched key and an unpublished one both read as `""`, and the difference matters: the
     * first is fixable here, the second is not.
     *
     * So a blank value earns exactly one fetch before it is believed. Nothing else in the app
     * refreshes remote config — `AppUpdateChecker` would, but nothing calls it — which would
     * otherwise leave this reading `""` forever no matter what is published in the console.
     *
     * Doing it here rather than at startup keeps the fix next to the value that needs it: no
     * ordering to get right, and a later launch reads the SDK's cached value without a round trip.
     */
    private suspend fun endpoint(): String {
        val published = remoteConfig.getString(ENDPOINT_KEY)
        if (published.isNotBlank()) return published

        remoteConfig.refresh()
        return remoteConfig.getString(ENDPOINT_KEY)
    }

    private suspend fun post(
        endpoint: String,
        attestationToken: String,
        request: RecipeRequest
    ): Result<List<Recipe>> = try {
        val response = client.post(endpoint) {
            header(APP_CHECK_HEADER, attestationToken)
            contentType(ContentType.Application.Json)
            setBody(request.toDto())
        }

        if (response.status.isSuccess()) {
            Result.success(response.body<SuggestResponseDto>().toDomain())
        } else {
            // The body carries an error code meant for our logs, not for the user, and the status
            // is the only part a caller could act on.
            Result.failure(IOException("Recipe request failed with ${response.status}"))
        }
    } catch (exception: IOException) {
        // Covers timeouts too: Ktor's timeout exception is one of these.
        Result.failure(exception)
    } catch (exception: ContentConvertException) {
        Result.failure(exception)
    } catch (exception: SerializationException) {
        Result.failure(exception)
    } catch (exception: NoTransformationFoundException) {
        Result.failure(exception)
    }

    private companion object {
        /**
         * Published per Firebase project. Absent in a project that has not had the function
         * deployed yet, which reads as "the feature is off" rather than as a crash.
         */
        const val ENDPOINT_KEY = "recipes_endpoint"

        /** What the Firebase App Check backend SDK reads on the server side. */
        const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
    }
}
