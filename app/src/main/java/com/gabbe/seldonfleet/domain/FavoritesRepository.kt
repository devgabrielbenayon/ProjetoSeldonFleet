package com.gabbe.seldonfleet.domain

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Ids favoritados; emite de novo a cada mudança. */
    val favorites: Flow<Set<String>>

    suspend fun toggle(id: String)
}
