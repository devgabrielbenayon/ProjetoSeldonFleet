package com.gabbe.seldonfleet.data

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetParserTest {

    @Test
    fun `campo ausente no JSON nao quebra o parse`() {
        val devices = parseFleet("""{ "devices": [ { "id": "DEV-1", "model": "X" } ] }""")
        assertEquals(1, devices.size)
        assertNull(devices[0].batteryCapacityMah)
        assertNull(devices[0].energyConsumedMah)
    }

    @Test
    fun `null explicito no JSON e aceito`() {
        val devices = parseFleet("""{ "devices": [ { "id": "DEV-1", "screenOnMinutes": null } ] }""")
        assertNull(devices.single().screenOnMinutes)
    }

    @Test
    fun `chaves desconhecidas sao ignoradas`() {
        val devices = parseFleet(
            """{ "schemaVersion": "1.0", "devices": [ { "id": "DEV-1", "ramMb": 4096, "novoCampo": true } ] }""",
        )
        assertEquals("DEV-1", devices.single().id)
    }

    @Test
    fun `numero inteiro e aceito em campo decimal`() {
        val devices = parseFleet(
            """{ "devices": [ { "id": "DEV-1", "energyConsumed": 980, "energyUnit": "mAh", "jankFramesPct": 3 } ] }""",
        )
        assertEquals(980.0, devices.single().energyConsumedMah!!, 1e-9)
        assertEquals(3.0, devices.single().jankFramesPct!!, 1e-9)
    }

    @Test
    fun `registro sem id e descartado e os outros seguem`() {
        val devices = parseFleet("""{ "devices": [ { "model": "sem id" }, { "id": "DEV-2" } ] }""")
        assertEquals(listOf("DEV-2"), devices.map { it.id })
    }

    @Test
    fun `lista de devices ausente vira lista vazia`() =
        assertTrue(parseFleet("""{ "fleetId": "X" }""").isEmpty())

    @Test(expected = SerializationException::class)
    fun `JSON malformado falha explicitamente`() {
        parseFleet("""{ "devices": [ """)
    }
}
