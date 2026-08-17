package com.appotato.features.recipes.implementation

import kotlinx.datetime.LocalDate

/**
 * What is about to go off, soonest first.
 *
 * The domain boundary over storage, same idea as the pantry's own repository: Room lives behind it
 * and nothing here mentions rows or queries.
 */
internal interface ExpiringItems {

    /**
     * Everything still edible, soonest to expire first, capped at [limit].
     *
     * Already-expired items are left out rather than passed on. The backend ignores them anyway —
     * a recipe built on food past its date is a safety problem, not a taste one — so sending them
     * would only be paying tokens to have them thrown away.
     *
     * Deciding which of these count as *urgent* is not this type's job; it returns the pantry in
     * date order and lets the caller draw the line.
     */
    suspend fun soonestFirst(today: LocalDate, limit: Int): List<ExpiringItem>
}
