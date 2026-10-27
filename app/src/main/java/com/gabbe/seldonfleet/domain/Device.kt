package com.gabbe.seldonfleet.domain

/**
 * Aparelho da frota já normalizado pela camada de dados.
 * Só o id é obrigatório; nos demais, null significa "ausente no arquivo" e nunca
 * é trocado por um valor inventado. Nos campos da regra, ausência → INDETERMINADO.
 */
data class Device(
    val id: String,
    val model: String?,
    val soc: String?,
    val tier: String?,
    val androidApi: Int?,
    val batteryLevelPct: Int?,
    val batteryCapacityMah: Int?,
    val screenOnMinutes: Int?,
    // Sempre em mAh: a conversão de mWh acontece antes de chegar aqui.
    // null = ausente ou impossível converter (unidade desconhecida, tensão inválida).
    val energyConsumedMah: Double?,
    val jankFramesPct: Double?,
    val crashCount7d: Int?,
    val lastSeen: String?,
    // Unidade em que o aparelho reportou o consumo. Só para exibição/auditoria:
    // a regra usa exclusivamente energyConsumedMah.
    val energyReportedUnit: String? = null,
)
