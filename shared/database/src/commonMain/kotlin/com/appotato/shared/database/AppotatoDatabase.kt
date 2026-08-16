package com.appotato.shared.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The app's single Room database.
 *
 * It lives in `shared/` rather than in a feature because there is exactly one database file, one
 * connection and one migration history for the whole app — a feature that owned its own would give
 * the next feature a second one. The rows here are storage records; what they *mean* belongs to the
 * feature that maps them, so no rule about food or expiry lives in this module.
 */
@Database(entities = [PantryItemEntity::class], version = 2)
@ConstructedBy(AppotatoDatabaseConstructor::class)
public abstract class AppotatoDatabase : RoomDatabase() {
    public abstract fun pantryItemDao(): PantryItemDao
}

/**
 * Room generates the `actual` for every target. The suppression is required: the compiler cannot
 * see an actual that the KSP processor has not written yet.
 */
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public expect object AppotatoDatabaseConstructor : RoomDatabaseConstructor<AppotatoDatabase> {
    override fun initialize(): AppotatoDatabase
}

internal const val DATABASE_FILE_NAME: String = "appotato.db"
