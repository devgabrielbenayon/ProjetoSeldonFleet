package com.gabbe.seldonfleet

import android.content.Context
import com.gabbe.seldonfleet.data.AssetDeviceRepository
import com.gabbe.seldonfleet.data.DataStoreFavoritesRepository
import com.gabbe.seldonfleet.data.favoritesDataStore
import com.gabbe.seldonfleet.domain.ClassifyDevice
import com.gabbe.seldonfleet.domain.DeviceRepository
import com.gabbe.seldonfleet.domain.FavoritesRepository

/**
 * Injeção de dependência manual: o único lugar que conhece as implementações concretas.
 * O resto do app enxerga só as interfaces do domínio.
 */
class AppContainer(context: Context) {
    val deviceRepository: DeviceRepository = AssetDeviceRepository(context.assets)
    val favoritesRepository: FavoritesRepository =
        DataStoreFavoritesRepository(context.favoritesDataStore)
    val classifyDevice = ClassifyDevice()
}
