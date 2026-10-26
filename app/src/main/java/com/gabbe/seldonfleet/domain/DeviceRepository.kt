package com.gabbe.seldonfleet.domain

// A interface mora no domínio (quem usa); a camada de dados a implementa.
interface DeviceRepository {
    suspend fun getDevices(): List<Device>
}
