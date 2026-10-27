package com.gabbe.seldonfleet.work

import android.app.ActivityManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FleetRefreshScheduler {
    private const val WORK_NAME = "fleet-refresh"

    fun schedule(context: Context) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val deviceClass = deviceClassFor(
            totalRamMb = memory.totalMem / (1024 * 1024),
            isLowRamDevice = activityManager.isLowRamDevice,
        )
        val policy = refreshPolicyFor(deviceClass)

        val request = PeriodicWorkRequestBuilder<FleetRefreshWorker>(policy.intervalHours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(policy.requiresBatteryNotLow)
                    .setRequiresCharging(policy.requiresCharging)
                    .build(),
            )
            .build()

        // KEEP: abrir o app de novo não recria nem reinicia o agendamento existente.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
