package com.gabbe.seldonfleet.domain

/**
 * Caso de uso: aplica a regra de classificação a um aparelho.
 * Os limiares vêm de fora para a regra poder ser ajustada sem mexer na lógica.
 */
class ClassifyDevice(
    private val thresholds: Thresholds = Thresholds(),
) {
    operator fun invoke(device: Device): Classification {
        // 1. Dado ausente ou inválido → INDETERMINADO antes de qualquer conta (sem NaN/Infinity)
        val capacity = device.batteryCapacityMah
        if (capacity == null || capacity <= 0) {
            return indeterminate(Signal.CAPACIDADE_INVALIDA, capacity?.toDouble())
        }
        val screenOn = device.screenOnMinutes
        if (screenOn == null || screenOn <= 0) {
            return indeterminate(Signal.TELA_INVALIDA, screenOn?.toDouble())
        }
        val energy = device.energyConsumedMah
            ?: return indeterminate(Signal.ENERGIA_AUSENTE, null)
        val crashes = device.crashCount7d
            ?: return indeterminate(Signal.FALHAS_AUSENTES, null)
        val jank = device.jankFramesPct
            ?: return indeterminate(Signal.QUADROS_PERDIDOS_AUSENTES, null)

        // 2. Smart cast: daqui em diante capacity e screenOn são Int > 0.
        // Multiplicar antes de dividir evita erro de arredondamento na fronteira
        // (ex.: 720 mAh, 60 min, 6000 mAh dá exatamente 12.0, não 11.999...).
        val drainMahPerHour = energy * 60.0 / screenOn
        val drainPctPerHour = drainMahPerHour * 100.0 / capacity

        fun classified(status: DeviceStatus, reason: Reason?) =
            Classification(status, reason, drainPctPerHour, drainMahPerHour)

        // 3. Nível crítico antes do de atenção; dentro do nível, a ordem é dreno → falhas → quadros
        val critical = firstTriggered(
            drainPctPerHour, crashes, jank,
            thresholds.criticalDrain, thresholds.criticalCrashes, thresholds.criticalJank,
        )
        if (critical != null) return classified(DeviceStatus.CRITICO, critical)

        val warning = firstTriggered(
            drainPctPerHour, crashes, jank,
            thresholds.warningDrain, thresholds.warningCrashes, thresholds.warningJank,
        )
        if (warning != null) return classified(DeviceStatus.ATENCAO, warning)

        return classified(DeviceStatus.OK, reason = null)
    }

    /** Primeiro sinal que atinge o limiar (>=), ou null se nenhum atingir. */
    private fun firstTriggered(
        drain: Double,
        crashes: Int,
        jank: Double,
        drainLimit: Double,
        crashLimit: Int,
        jankLimit: Double,
    ): Reason? = when {
        drain >= drainLimit -> Reason(Signal.DRENO, drain, drainLimit)
        crashes >= crashLimit -> Reason(Signal.FALHAS, crashes.toDouble(), crashLimit.toDouble())
        jank >= jankLimit -> Reason(Signal.QUADROS_PERDIDOS, jank, jankLimit)
        else -> null
    }

    private fun indeterminate(signal: Signal, observed: Double?) = Classification(
        status = DeviceStatus.INDETERMINADO,
        reason = Reason(signal, observed, threshold = null),
        drainPctPerHour = null,
        drainMahPerHour = null,
    )
}
