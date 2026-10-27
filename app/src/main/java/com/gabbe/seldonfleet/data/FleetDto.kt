package com.gabbe.seldonfleet.data

import kotlinx.serialization.Serializable

// Espelho fiel do JSON. Tudo anulável com padrão null: campo ausente não quebra o parse,
// e a decisão sobre o que fazer com a ausência fica no mapper.

@Serializable
data class FleetDto(
    val devices: List<DeviceDto> = emptyList(),
)

@Serializable
data class DeviceDto(
    val id: String? = null,
    val model: String? = null,
    val soc: String? = null,
    val tier: String? = null,
    val androidApi: Int? = null,
    val batteryCapacityMah: Int? = null,
    val batteryLevelPct: Int? = null,
    val screenOnMinutes: Int? = null,
    val energyConsumed: Double? = null,
    val energyUnit: String? = null,
    val nominalVoltage: Double? = null,
    val jankFramesPct: Double? = null,
    val crashCount7d: Int? = null,
    val lastSeen: String? = null,
)
