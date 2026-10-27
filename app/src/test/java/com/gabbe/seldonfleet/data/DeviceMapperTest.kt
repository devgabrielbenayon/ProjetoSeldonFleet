package com.gabbe.seldonfleet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceMapperTest {

    // ---------- Conversão de unidade (defeito 1) ----------

    @Test
    fun `mWh e convertido para mAh dividindo pela tensao`() =
        assertEquals(980.0, energyInMah(3773.0, "mWh", 3.85)!!, 1e-9)

    @Test
    fun `mAh passa intacto`() =
        assertEquals(980.0, energyInMah(980.0, "mAh", 3.85)!!, 1e-9)

    @Test
    fun `mAh nao depende da tensao`() =
        assertEquals(980.0, energyInMah(980.0, "mAh", null)!!, 1e-9)

    @Test
    fun `mWh sem tensao vira null`() = assertNull(energyInMah(3773.0, "mWh", null))

    @Test
    fun `mWh com tensao zero vira null`() = assertNull(energyInMah(3773.0, "mWh", 0.0))

    @Test
    fun `mWh com tensao negativa vira null`() = assertNull(energyInMah(3773.0, "mWh", -3.85))

    @Test
    fun `unidade desconhecida vira null`() = assertNull(energyInMah(3.773, "Wh", 3.85))

    @Test
    fun `unidade com maiuscula errada vira null`() = assertNull(energyInMah(3773.0, "MWh", 3.85))

    @Test
    fun `unidade ausente vira null`() = assertNull(energyInMah(980.0, null, 3.85))

    @Test
    fun `energia ausente vira null`() = assertNull(energyInMah(null, "mAh", 3.85))

    // ---------- DTO → Device ----------

    private val dto = DeviceDto(
        id = "DEV-0204", model = "Moto G84", soc = "Snapdragon 695", tier = "mid",
        androidApi = 34, batteryCapacityMah = 5000, batteryLevelPct = 61,
        screenOnMinutes = 186, energyConsumed = 3773.0, energyUnit = "mWh",
        nominalVoltage = 3.85, jankFramesPct = 4.1, crashCount7d = 0,
        lastSeen = "2026-10-20T02:50:00Z",
    )

    @Test
    fun `mapper converte a energia e copia os demais campos`() {
        val device = dto.toDomain()!!
        assertEquals(980.0, device.energyConsumedMah!!, 1e-9)
        assertEquals("DEV-0204", device.id)
        assertEquals(5000, device.batteryCapacityMah)
        assertEquals(186, device.screenOnMinutes)
        assertEquals(4.1, device.jankFramesPct!!, 1e-9)
        assertEquals(0, device.crashCount7d)
    }

    @Test
    fun `registro sem id e descartado`() = assertNull(dto.copy(id = null).toDomain())

    @Test
    fun `registro com id em branco e descartado`() = assertNull(dto.copy(id = "  ").toDomain())

    @Test
    fun `registro so com id vira Device com campos nulos`() {
        val device = DeviceDto(id = "DEV-X").toDomain()
        assertNotNull(device)
        assertNull(device!!.batteryCapacityMah)
        assertNull(device.energyConsumedMah)
        assertNull(device.crashCount7d)
        assertNull(device.model)
    }
}
