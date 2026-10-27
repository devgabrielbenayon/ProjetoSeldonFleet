package com.gabbe.seldonfleet.work

// Política da atualização periódica em Kotlin puro: testável na JVM, sem WorkManager.
// Números justificados em TRILHA_C.md.

enum class DeviceClass { LOW_END, STANDARD }

data class RefreshPolicy(
    val intervalHours: Long,
    val requiresBatteryNotLow: Boolean,
    val requiresCharging: Boolean,
)

// Na frota, todo aparelho low tem até 4 GB de RAM e todo mid/high tem 6 GB ou mais.
// O Android reporta um pouco menos que o nominal (4 GB ≈ 3,7 GB), por isso o corte em 5 GB.
private const val LOW_END_MAX_RAM_MB = 5 * 1024L

fun deviceClassFor(totalRamMb: Long, isLowRamDevice: Boolean): DeviceClass =
    if (isLowRamDevice || totalRamMb < LOW_END_MAX_RAM_MB) DeviceClass.LOW_END else DeviceClass.STANDARD

fun refreshPolicyFor(deviceClass: DeviceClass): RefreshPolicy = when (deviceClass) {
    // SoC menos eficiente: cada execução custa mais. Só roda carregando, 1x por dia.
    DeviceClass.LOW_END -> RefreshPolicy(intervalHours = 24, requiresBatteryNotLow = true, requiresCharging = true)
    DeviceClass.STANDARD -> RefreshPolicy(intervalHours = 12, requiresBatteryNotLow = true, requiresCharging = false)
}
