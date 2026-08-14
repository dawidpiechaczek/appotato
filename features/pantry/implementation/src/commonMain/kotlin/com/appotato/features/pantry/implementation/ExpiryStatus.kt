package com.appotato.features.pantry.implementation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** How urgent an item is. Derived from the date and today, never stored. */
internal enum class ExpiryStatus {
    Expired,
    ExpiringSoon,
    Fresh
}

/** Anything due within this many days counts as urgent. */
internal const val EXPIRING_SOON_DAYS: Int = 3

internal fun PantryItem.statusOn(today: LocalDate): ExpiryStatus = when {
    today.daysUntil(expiresOn) < 0 -> ExpiryStatus.Expired
    today.daysUntil(expiresOn) <= EXPIRING_SOON_DAYS -> ExpiryStatus.ExpiringSoon
    else -> ExpiryStatus.Fresh
}
