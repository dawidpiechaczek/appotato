package com.appotato.features.pantry.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [PantryRepository], sorted the way the DAO's `ORDER BY` sorts. */
internal class PantryRepositoryFake(initial: List<PantryItem> = emptyList()) : PantryRepository {

    private val items = MutableStateFlow(initial)

    val current: List<PantryItem> get() = items.value

    override fun observeItems(): Flow<List<PantryItem>> =
        items.map { list -> list.sortedWith(compareBy({ it.expiresOn }, { it.name })) }

    override suspend fun count(): Int = items.value.size

    override suspend fun add(item: PantryItem) {
        items.value = items.value.filterNot { it.id == item.id } + item
    }

    override suspend fun remove(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}
