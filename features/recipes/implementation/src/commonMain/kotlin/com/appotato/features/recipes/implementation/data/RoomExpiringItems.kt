package com.appotato.features.recipes.implementation.data

import com.appotato.features.recipes.implementation.ExpiringItem
import com.appotato.features.recipes.implementation.ExpiringItems
import com.appotato.shared.database.PantryItemDao
import com.appotato.shared.database.PantryItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Reads the same table the pantry does, through the same DAO, and maps it onto this feature's own
 * model — which is what keeps recipes from depending on the pantry feature.
 */
internal class RoomExpiringItems(private val dao: PantryItemDao) : ExpiringItems {

    override suspend fun soonestFirst(today: LocalDate, limit: Int): List<ExpiringItem> {
        val from = today.toEpochDays()

        // The DAO exposes a Flow because the pantry list watches it; here a single read is all the
        // screen needs, and observing would mean re-asking the model every time an item is added.
        return dao.observeExpiringFrom(from)
            .first()
            .take(limit)
            .map { row -> row.toExpiringItem(from) }
    }
}

private fun PantryItemEntity.toExpiringItem(todayEpochDay: Long): ExpiringItem = ExpiringItem(
    id = id,
    name = name,
    ingredientCode = ingredientCode,
    // The query's lower bound is today, so this can never be negative.
    daysUntilExpiry = (expiresOnEpochDay - todayEpochDay).toInt()
)
