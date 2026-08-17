package com.appotato.features.recipes.implementation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A pantry item as the recipes screen needs to see it — a name to cook with, a code to group by,
 * and how long there is left.
 *
 * This is not the pantry's `PantryItem` and it deliberately cannot be: features never depend on
 * each other's implementation modules. `:shared:database` stores rows and holds no opinion about
 * what they mean, so each feature maps them onto its own model, and this one has no use for the
 * category, the barcode, the photo or the calories.
 */
internal data class ExpiringItem(
    val id: String,
    val name: String,
    /** Null when nothing could be resolved; the name is then the only thing to go on. */
    val ingredientCode: String?,
    /** Zero means today. Never negative — expired items are excluded before this is built. */
    val daysUntilExpiry: Int
)

/**
 * How soon something has to be going off before this screen has anything to say.
 *
 * The same three days the pantry badges as "expiring soon" — duplicated because that constant is
 * `internal` to the pantry feature, and two features may not depend on each other's implementation.
 * The number is a product decision and the two must not drift: an app that badges two items as
 * urgent and then offers recipes "from five items about to expire" is contradicting itself.
 */
internal const val RECIPE_TRIGGER_DAYS: Int = 3

/**
 * A ceiling on how much of the pantry gets sent, soonest-expiring first.
 *
 * The basket is deliberately the whole pantry rather than the urgent slice — the model is told to
 * build *around* what expires first, and giving it more to work with is what turns one lonely
 * carton of milk into something cookable. The cap exists because prompt length is money, and it
 * matches the backend's own limit so the truncation happens here, visibly, rather than there.
 */
internal const val MAX_RECIPE_INGREDIENTS: Int = 40

/**
 * Today's date, injected rather than read inline so a test can sit on either side of a day boundary
 * without waiting for midnight. Same shape as the pantry's, which is `internal` to that feature.
 */
internal fun interface Today {
    operator fun invoke(): LocalDate
}

internal class SystemToday : Today {

    // kotlinx-datetime 0.7 hands Clock and Instant back to the standard library, where they are
    // still experimental. The opt-in stays here rather than spreading through the feature.
    @OptIn(ExperimentalTime::class)
    override fun invoke(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
