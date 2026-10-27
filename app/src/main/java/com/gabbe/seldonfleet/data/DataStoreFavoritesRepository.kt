package com.gabbe.seldonfleet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gabbe.seldonfleet.domain.FavoritesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Uma instância por processo (exigência do DataStore); o arquivo fica em files/datastore/.
val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

/** Recebe o DataStore pronto, e não o Context, para poder ser testado na JVM com um arquivo temporário. */
class DataStoreFavoritesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {

    override val favorites: Flow<Set<String>> = dataStore.data
        // Arquivo ilegível não derruba o app: segue sem favoritos.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[FAVORITE_IDS] ?: emptySet() }

    // edit é transacional: ler e gravar acontecem juntos, sem perder toques rápidos.
    override suspend fun toggle(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS] ?: emptySet()
            prefs[FAVORITE_IDS] = if (id in current) current - id else current + id
        }
    }

    private companion object {
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_ids")
    }
}
