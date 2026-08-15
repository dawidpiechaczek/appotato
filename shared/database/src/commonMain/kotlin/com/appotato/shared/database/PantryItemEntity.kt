package com.appotato.shared.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A row, not a domain object. The feature maps this onto its own `PantryItem` — which is why the
 * date is a plain epoch day here and a `LocalDate` there, and why [category] is an opaque code
 * rather than an enum: this module stores rows and holds no opinion about what a category means.
 *
 * Storing the date as a number rather than through a Room `TypeConverter` keeps the column sortable
 * and comparable in SQL, so "what expires in the next three days" stays a query instead of a filter
 * over everything.
 */
@Entity(
    tableName = "pantry_items",
    // The list is always read in this order; without the index every emission is a full sort.
    indices = [Index(value = ["expires_on_epoch_day"])]
)
public data class PantryItemEntity(
    @PrimaryKey
    public val id: String,
    public val name: String,
    @ColumnInfo(name = "expires_on_epoch_day")
    public val expiresOnEpochDay: Long,
    /** Feature-owned code. Unknown values must map back to a fallback, never crash. */
    public val category: String,
    public val quantity: String,
    /** Null for anything typed in by hand rather than scanned. */
    public val barcode: String? = null
)
