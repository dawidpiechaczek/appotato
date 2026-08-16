package com.appotato.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import okio.Path
import org.koin.core.KoinApplication

/**
 * Where the downloaded product photos live. Both platforms put it under the directory the system
 * treats as a cache — the OS may empty it under pressure, it is not backed up, and it shows up as
 * cache rather than app data in the system's storage screen.
 */
internal expect fun imageCacheDirectory(context: PlatformContext): Path

internal const val IMAGE_CACHE_DIRECTORY: String = "image_cache"

/**
 * A ceiling, not a reservation: the cache only ever holds photos the app has actually shown, and
 * those are ~10 kB thumbnails. Coil's own default tops out at 250 MB, which is far more than this
 * app could fill and reads alarmingly in the storage screen if it ever did.
 */
private const val MAX_IMAGE_CACHE_BYTES = 64L * 1024 * 1024

/** Two percent of what is free, so a nearly full phone gets a smaller cache rather than the cap. */
private const val IMAGE_CACHE_FREE_SPACE_SHARE = 0.02

/**
 * Points Coil at the app's own `HttpClient` and at a cache directory this app chose.
 *
 * Coil registers a network fetcher by itself as soon as `coil-network-ktor3` is on the classpath,
 * but that one calls `HttpClient()` — a second engine, a second connection pool, and none of the
 * timeouts, retries or User-Agent configured in `:shared:network`. Handing it ours makes image
 * loading behave like every other request the app makes.
 *
 * Both caches are on, which is what makes the pantry work offline: Coil's default read strategy
 * serves a photo straight off disk without going to the network at all, so a saved item keeps its
 * picture with no connection. Coil would enable them by itself, but its default directory is the
 * temporary one, which iOS may empty between launches — the point of the cache is that it survives.
 *
 * The lambda runs on first use, not now, so it can resolve out of a Koin graph that is only just
 * being assembled. `setSafe` rather than `set`: it leaves an already-installed loader alone.
 */
internal fun KoinApplication.setupImageLoader() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(koin.get<HttpClient>())) }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDirectory(context))
                    .maxSizePercent(IMAGE_CACHE_FREE_SPACE_SHARE)
                    .maximumMaxSizeBytes(MAX_IMAGE_CACHE_BYTES)
                    .build()
            }
            .build()
    }
}
