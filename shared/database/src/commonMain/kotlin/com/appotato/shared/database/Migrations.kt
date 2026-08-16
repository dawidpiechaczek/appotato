package com.appotato.shared.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Every schema change the app has shipped, in order. New ones are appended here and nowhere else.
 *
 * They are written by hand rather than left to destructive migration: the pantry is the only copy
 * of the user's data, and dropping it to add a column is not a trade this app gets to make.
 *
 * Until the app is in a store there is no installed version to be compatible with, so a change that
 * has not shipped is folded into the version being worked on rather than given its own. Once it has
 * shipped, that stops: from then on every released version needs its own step, because somewhere a
 * device is sitting on it.
 */
internal fun RoomDatabase.Builder<AppotatoDatabase>.addAppotatoMigrations():
    RoomDatabase.Builder<AppotatoDatabase> = addMigrations(MigrateScanColumns)

/**
 * v2 — everything a barcode scan fills in: calories per 100 g and the URL of a product photo.
 *
 * Both nullable and with no default, so rows that predate the scanner keep meaning "unknown" rather
 * than being recorded as a zero-calorie food with a broken image.
 *
 * The photo is a URL and not the image itself: the bytes belong in the image cache, which can evict
 * them, and a row that carried them would drag megabytes through memory on every list query.
 */
private object MigrateScanColumns : Migration(startVersion = 1, endVersion = 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE pantry_items ADD COLUMN calories_per_100g INTEGER")
        connection.execSQL("ALTER TABLE pantry_items ADD COLUMN image_url TEXT")
    }
}
