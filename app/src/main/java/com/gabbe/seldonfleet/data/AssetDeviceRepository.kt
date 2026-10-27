package com.gabbe.seldonfleet.data

import android.content.res.AssetManager
import com.gabbe.seldonfleet.domain.Device
import com.gabbe.seldonfleet.domain.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Única peça que toca o Android para ler a frota; toda a lógica fica em [parseFleet]. */
class AssetDeviceRepository(
    private val assets: AssetManager,
    private val fileName: String = "frota.json",
) : DeviceRepository {

    // Leitura de arquivo fora da thread principal para não travar a tela.
    override suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val text = assets.open(fileName).bufferedReader().use { it.readText() }
        parseFleet(text)
    }
}
