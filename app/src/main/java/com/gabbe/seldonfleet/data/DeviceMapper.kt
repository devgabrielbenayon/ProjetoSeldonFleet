package com.gabbe.seldonfleet.data

import com.gabbe.seldonfleet.domain.Device

/**
 * Converte o DTO no modelo de domínio. Devolve null se não houver id
 * (sem id não há como navegar nem favoritar o aparelho).
 */
fun DeviceDto.toDomain(): Device? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    return Device(
        id = id,
        model = model,
        soc = soc,
        tier = tier,
        androidApi = androidApi,
        batteryLevelPct = batteryLevelPct,
        batteryCapacityMah = batteryCapacityMah,
        screenOnMinutes = screenOnMinutes,
        energyConsumedMah = energyInMah(energyConsumed, energyUnit, nominalVoltage),
        jankFramesPct = jankFramesPct,
        crashCount7d = crashCount7d,
        lastSeen = lastSeen,
        energyReportedUnit = energyUnit,
    )
}

/**
 * Normaliza a energia para mAh (defeito 1 do arquivo).
 * mWh → mAh dividindo pela tensão nominal (E = Q × V ⇒ Q = E ÷ V).
 * Unidade desconhecida ou tensão inválida → null (o domínio classifica como INDETERMINADO).
 */
internal fun energyInMah(value: Double?, unit: String?, nominalVoltage: Double?): Double? {
    if (value == null) return null
    // Comparação exata, com maiúsculas: "MWh" (megawatt-hora) não é "mWh".
    return when (unit) {
        "mAh" -> value
        "mWh" -> nominalVoltage?.takeIf { it > 0.0 }?.let { value / it }
        else -> null
    }
}
