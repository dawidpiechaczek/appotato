package com.appotato.shared.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.appotato.shared.dispatchers.CoroutineDispatchers
import org.koin.core.module.Module

/**
 * Binds [AppotatoDatabase] and its DAOs. Expect/actual because only the platform knows where an
 * app is allowed to put a file — `getDatabasePath` on Android, the documents directory on iOS.
 */
public expect fun databaseModule(): Module

/**
 * Everything about the connection that is not the file path.
 *
 * The bundled SQLite driver ships its own copy of the engine, so both platforms run the same
 * version instead of whatever the OS happens to provide.
 */
internal fun RoomDatabase.Builder<AppotatoDatabase>.buildWith(
    dispatchers: CoroutineDispatchers
): AppotatoDatabase = setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(dispatchers.io)
    .addAppotatoMigrations()
    .build()
