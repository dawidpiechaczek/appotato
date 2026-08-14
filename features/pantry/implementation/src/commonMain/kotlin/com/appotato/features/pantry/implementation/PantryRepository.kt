package com.appotato.features.pantry.implementation

import kotlinx.coroutines.flow.Flow

/**
 * The domain boundary over storage. Room lives behind it, and so would a backend later — which is
 * why nothing here mentions rows, queries or documents.
 */
internal interface PantryRepository {

    /** Soonest to expire first. Emits again on every change. */
    fun observeItems(): Flow<List<PantryItem>>

    /** Cheaper than counting [observeItems]; the free-tier check runs before every insert. */
    suspend fun count(): Int

    suspend fun add(item: PantryItem)

    suspend fun remove(id: String)
}
