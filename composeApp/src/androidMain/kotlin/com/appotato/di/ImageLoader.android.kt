package com.appotato.di

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * `cacheDir`, so "Clear cache" in the system settings reaches it and the photos never count as user
 * data — everything in here can be downloaded again.
 */
internal actual fun imageCacheDirectory(context: PlatformContext): Path =
    context.cacheDir.resolve(IMAGE_CACHE_DIRECTORY).toOkioPath()
