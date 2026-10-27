package com.gabbe.seldonfleet.data

import com.gabbe.seldonfleet.domain.Device
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Função pura: texto JSON → aparelhos do domínio. Sem Android, testável na JVM.
 * JSON malformado lança SerializationException: falha explícita, tratada por quem chama.
 */
fun parseFleet(text: String): List<Device> =
    json.decodeFromString<FleetDto>(text).devices.mapNotNull { it.toDomain() }
