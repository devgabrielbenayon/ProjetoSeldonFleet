package com.gabbe.seldonfleet.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabbe.seldonfleet.SeldonFleetApp
import kotlinx.coroutines.CancellationException

/** Relê a frota pelo DeviceRepository (mesma interface da UI) e atualiza o cache. */
class FleetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SeldonFleetApp).container
        return try {
            val devices = container.deviceRepository.getDevices()
            container.fleetCache.update(devices, System.currentTimeMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // failure e não retry: o asset não muda, então tentar de novo só gastaria bateria.
            Result.failure()
        }
    }
}
