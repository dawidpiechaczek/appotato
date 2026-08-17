package com.appotato.shared.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
public interface PantryItemDao {

    /** Soonest to expire first — the only order the pantry screen ever wants. */
    @Query("SELECT * FROM pantry_items ORDER BY expires_on_epoch_day ASC, name ASC")
    public fun observeAll(): Flow<List<PantryItemEntity>>

    /**
     * Everything not yet past its date, soonest first.
     *
     * A comparison in SQL rather than a filter in Kotlin, which is the whole reason the date is
     * stored as a sortable number instead of going through a `TypeConverter`. What counts as urgent
     * is the caller's business; this module only knows how to read rows in date order.
     */
    @Query(
        "SELECT * FROM pantry_items " +
            "WHERE expires_on_epoch_day >= :fromEpochDay " +
            "ORDER BY expires_on_epoch_day ASC, name ASC"
    )
    public fun observeExpiringFrom(fromEpochDay: Long): Flow<List<PantryItemEntity>>

    @Query("SELECT COUNT(*) FROM pantry_items")
    public suspend fun count(): Int

    @Upsert
    public suspend fun upsert(item: PantryItemEntity)

    @Delete
    public suspend fun delete(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    public suspend fun deleteById(id: String)
}
