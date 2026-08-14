package com.appotato.shared.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row, not a domain object. The feature maps this onto its own `PantryItem` — which is why the
 * date is a plain epoch day here and a `LocalDate` there.
 *
 * Storing the date as a number rather than through a Room `TypeConverter` keeps the column sortable
 * and comparable in SQL, so "what expires in the next three days" stays a query instead of a filter
 * over everything.
 */
@Entity(tableName = "pantry_items")
public data class PantryItemEntity(
    @PrimaryKey
    public val id: String,
    public val name: String,
    @ColumnInfo(name = "expires_on_epoch_day")
    public val expiresOnEpochDay: Long
)
