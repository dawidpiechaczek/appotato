package com.appotato.shared.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

public actual fun databaseModule(): Module = module {
    single { databaseBuilder().buildWith(get()) }
    single { get<AppotatoDatabase>().pantryItemDao() }
}

private fun databaseBuilder(): RoomDatabase.Builder<AppotatoDatabase> =
    Room.databaseBuilder<AppotatoDatabase>(name = "${documentDirectory()}/$DATABASE_FILE_NAME")

/**
 * Documents and not caches: the pantry is user data, and the system may empty caches at any time.
 */
@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) { "Could not resolve the iOS documents directory" }
}
