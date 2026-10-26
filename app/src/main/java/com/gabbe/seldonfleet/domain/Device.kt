package com.gabbe.seldonfleet.domain

/**
 * Aparelho da frota já normalizado pela camada de dados.
 * Campos que a regra usa são anuláveis: null significa "ausente no arquivo",
 * e a regra classifica como INDETERMINADO em vez de supor um valor.
 */
data class Device(
    val id: String,
    val model: String,
    val soc: String,
    val tier: String,
    val androidApi: Int,
    val batteryLevelPct: Int,
    val batteryCapacityMah: Int?,
    val screenOnMinutes: Int?,
    // Sempre em mAh: a conversão de mWh acontece antes de chegar aqui.
    // null = ausente ou impossível converter (unidade desconhecida, tensão inválida).
    val energyConsumedMah: Double?,
    val jankFramesPct: Double?,
    val crashCount7d: Int?,
    val lastSeen: String,
)
