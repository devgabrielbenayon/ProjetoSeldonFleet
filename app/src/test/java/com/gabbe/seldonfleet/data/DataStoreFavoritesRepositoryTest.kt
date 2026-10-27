package com.gabbe.seldonfleet.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreFavoritesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "favorites.preferences_pb")

    private fun repository(scope: CoroutineScope) = DataStoreFavoritesRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = ::file),
    )

    @Test
    fun `comeca sem favoritos`() = runTest {
        val repo = repository(backgroundScope)
        assertEquals(emptySet<String>(), repo.favorites.first())
    }

    @Test
    fun `toggle adiciona e um segundo toggle remove`() = runTest {
        val repo = repository(backgroundScope)
        repo.toggle("DEV-0522")
        repo.toggle("DEV-1839")
        assertEquals(setOf("DEV-0522", "DEV-1839"), repo.favorites.first())
        repo.toggle("DEV-0522")
        assertEquals(setOf("DEV-1839"), repo.favorites.first())
    }

    @Test
    fun `favoritos sobrevivem a recriacao do DataStore (simula reabrir o app)`() = runTest {
        val firstJob = Job()
        repository(CoroutineScope(Dispatchers.IO + firstJob)).toggle("DEV-0522")
        firstJob.cancelAndJoin() // "fecha o app": libera o arquivo

        val reopened = repository(CoroutineScope(Dispatchers.IO + Job()))
        assertEquals(setOf("DEV-0522"), reopened.favorites.first())
    }
}
