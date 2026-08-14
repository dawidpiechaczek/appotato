package com.appotato.features.pantry.implementation.data

import com.appotato.features.pantry.implementation.PantryItem
import com.appotato.features.pantry.implementation.PantryRepository
import com.appotato.shared.database.PantryItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Ordering is the DAO's job — `ORDER BY expires_on_epoch_day` in SQL beats re-sorting the whole
 * list in Kotlin on every emission.
 */
internal class RoomPantryRepository(private val dao: PantryItemDao) : PantryRepository {

    override fun observeItems(): Flow<List<PantryItem>> =
        dao.observeAll().map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun count(): Int = dao.count()

    override suspend fun add(item: PantryItem): Unit = dao.upsert(item.toEntity())

    override suspend fun remove(id: String): Unit = dao.deleteById(id)
}
