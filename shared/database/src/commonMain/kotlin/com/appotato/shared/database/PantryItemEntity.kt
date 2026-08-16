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
    public val barcode: String? = null,
    /**
     * Null whenever the figure is unknown, which is most rows: it is filled in from a product
     * database after a scan, and nothing asks the user to type it. Per 100 g rather than per pack,
     * because that is the one basis every source states and the only one two items can be compared
     * on.
     */
    @ColumnInfo(name = "calories_per_100g")
    public val caloriesPer100g: Int? = null,
    /**
     * A remote photo of the packaging, or null for anything added by hand. The URL is stored rather
     * than the bytes: it stays valid because these image URLs are revision-stamped, and the image
     * cache is the layer that should decide what is worth keeping on disk.
     */
    @ColumnInfo(name = "image_url")
    public val imageUrl: String? = null,
    /**
     * A stable, language-neutral name for the food itself, resolved from the scan's tags or from
     * [name] — `milk`, never `Mleko UHT 3,2%`. Null whenever nothing could be resolved, which is an
     * ordinary outcome and not an error: the row is still perfectly usable, it just cannot be
     * grouped with the same food bought under a different brand or entered in another language.
     *
     * Opaque here on purpose, exactly like [category]: this module stores it and holds no opinion
     * about what any particular value means.
     */
    @ColumnInfo(name = "ingredient_code")
    public val ingredientCode: String? = null
)
