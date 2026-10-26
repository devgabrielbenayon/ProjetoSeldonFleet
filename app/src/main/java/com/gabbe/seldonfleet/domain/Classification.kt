package com.gabbe.seldonfleet.domain

/** Sinal que determinou o estado do aparelho. */
enum class Signal {
    DRENO,
    FALHAS,
    QUADROS_PERDIDOS,
    CAPACIDADE_INVALIDA,
    TELA_INVALIDA,
    ENERGIA_AUSENTE,
    FALHAS_AUSENTES,
    QUADROS_PERDIDOS_AUSENTES,
}

/**
 * Por que o aparelho recebeu o estado: qual sinal, valor observado e limiar.
 * observed/threshold são null quando não se aplicam (ex.: campo ausente).
 */
data class Reason(
    val signal: Signal,
    val observed: Double?,
    val threshold: Double?,
)

data class Classification(
    val status: DeviceStatus,
    val reason: Reason?,           // null quando OK
    val drainPctPerHour: Double?,  // null quando INDETERMINADO
    val drainMahPerHour: Double?,  // null quando INDETERMINADO
)

/** Limiares da regra num lugar só; os padrões são os de referência da especificação. */
data class Thresholds(
    val criticalDrain: Double = 12.0,
    val criticalCrashes: Int = 3,
    val criticalJank: Double = 15.0,
    val warningDrain: Double = 7.0,
    val warningCrashes: Int = 1,
    val warningJank: Double = 8.0,
)
