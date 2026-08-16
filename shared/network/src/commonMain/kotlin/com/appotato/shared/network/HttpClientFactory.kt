package com.appotato.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The engine is per-platform because it is the one part of Ktor that is not common code: OkHttp on
 * Android, `NSURLSession` on iOS. Everything above it — JSON, timeouts, retries — is configured
 * once, here, so a second API client cannot quietly get different behaviour.
 */
internal expect fun httpClientEngine(): HttpClientEngine

/**
 * Public APIs commonly reject or throttle callers that do not identify themselves, so the client
 * always sends a name rather than Ktor's default. The URL is the contact point they ask for.
 */
private const val USER_AGENT = "Appotato/1.0 (https://github.com/dawidpiechaczek/appotato)"

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

/** A phone loses its connection mid-request often enough that one blind retry is worth it. */
public const val DEFAULT_MAX_RETRIES: Int = 2

/**
 * `ignoreUnknownKeys` is not optional here: responses are third-party documents that gain fields
 * without notice, and the alternative is an app that starts failing on a server-side change.
 */
internal val AppotatoJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * Public so a test can hand in `MockEngine` and still exercise this exact configuration — a test
 * that builds its own client proves nothing about the one that ships.
 *
 * [maxRetries] is a parameter for the same reason: tests pass 0 so a failure surfaces immediately
 * instead of after several seconds of real backoff, and everything else takes the default.
 */
public fun appotatoHttpClient(
    engine: HttpClientEngine = httpClientEngine(),
    maxRetries: Int = DEFAULT_MAX_RETRIES
): HttpClient =
    HttpClient(engine) {
        // Left false so a 404 comes back as a response to inspect rather than an exception — "no
        // such product" is an answer, not a failure.
        expectSuccess = false

        install(ContentNegotiation) {
            json(AppotatoJson)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }

        install(HttpRequestRetry) {
            // Server errors and dropped connections only. A 4xx means the request itself is wrong,
            // and repeating it just spends the user's battery.
            retryOnServerErrors(maxRetries = maxRetries)
            retryOnException(maxRetries = maxRetries, retryOnTimeout = true)
            exponentialDelay()
        }

        install(UserAgent) {
            agent = USER_AGENT
        }
    }
