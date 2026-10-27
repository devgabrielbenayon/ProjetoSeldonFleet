package com.gabbe.seldonfleet.data

import com.gabbe.seldonfleet.domain.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FleetSnapshot(
    val devices: List<Device>,
    val refreshedAtMillis: Long,
)

/** Última leitura feita pela atualização periódica; vive enquanto o processo viver. */
class FleetCache {
    private val _snapshot = MutableStateFlow<FleetSnapshot?>(null)
    val snapshot: StateFlow<FleetSnapshot?> = _snapshot.asStateFlow()

    fun update(devices: List<Device>, refreshedAtMillis: Long) {
        _snapshot.value = FleetSnapshot(devices, refreshedAtMillis)
    }
}
