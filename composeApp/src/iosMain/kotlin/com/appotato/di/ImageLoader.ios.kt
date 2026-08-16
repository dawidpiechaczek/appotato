package com.appotato.di

import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Caches and not `tmp`: Apple's rule is that anything the app can download again belongs in
 * Library/Caches, which survives between launches and is still purgeable and excluded from backups.
 * `tmp` is for files that do not need to outlive the run, and the system empties it more eagerly —
 * which would cost the offline photos this cache exists for.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun imageCacheDirectory(context: PlatformContext): Path {
    val caches: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSCachesDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = requireNotNull(caches?.path) { "Could not resolve the iOS caches directory" }
    return path.toPath() / IMAGE_CACHE_DIRECTORY
}
