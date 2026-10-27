package com.gabbe.seldonfleet.data

import com.gabbe.seldonfleet.domain.ClassifyDevice
import com.gabbe.seldonfleet.domain.DeviceStatus
import com.gabbe.seldonfleet.domain.DeviceStatus.ATENCAO
import com.gabbe.seldonfleet.domain.DeviceStatus.CRITICO
import com.gabbe.seldonfleet.domain.DeviceStatus.INDETERMINADO
import com.gabbe.seldonfleet.domain.DeviceStatus.OK
import com.gabbe.seldonfleet.domain.Signal
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Integração: arquivo real → parser → mapper → regra. Roda na JVM, sem emulador. */
class FleetPipelineTest {

    // O teste roda com o diretório do módulo app/ como pasta atual.
    private val devices = parseFleet(File("src/main/assets/frota.json").readText())
    private val classify = ClassifyDevice()
    private val results = devices.associate { it.id to classify(it) }

    @Test
    fun `le os 30 aparelhos do arquivo`() = assertEquals(30, devices.size)

    @Test
    fun `contagem final e 16 OK, 8 ATENCAO, 4 CRITICO, 2 INDETERMINADO`() {
        val counts = results.values.groupingBy { it.status }.eachCount()
        assertEquals(mapOf(OK to 16, ATENCAO to 8, CRITICO to 4, INDETERMINADO to 2), counts)
    }

    @Test
    fun `cada aparelho bate com o gabarito`() {
        val expected: Map<String, DeviceStatus> = mapOf(
            "DEV-0137" to ATENCAO, "DEV-0204" to OK, "DEV-0311" to INDETERMINADO,
            "DEV-0418" to OK, "DEV-0522" to CRITICO, "DEV-0630" to OK,
            "DEV-0745" to INDETERMINADO, "DEV-0800" to OK, "DEV-0854" to OK,
            "DEV-0916" to OK, "DEV-0964" to OK, "DEV-1057" to OK,
            "DEV-1142" to OK, "DEV-1194" to OK, "DEV-1214" to OK,
            "DEV-1231" to OK, "DEV-1258" to OK, "DEV-1277" to OK,
            "DEV-1366" to OK, "DEV-1422" to OK, "DEV-1514" to ATENCAO,
            "DEV-1546" to ATENCAO, "DEV-1559" to ATENCAO, "DEV-1582" to ATENCAO,
            "DEV-1625" to ATENCAO, "DEV-1711" to ATENCAO, "DEV-1767" to ATENCAO,
            "DEV-1839" to CRITICO, "DEV-1924" to CRITICO, "DEV-1985" to CRITICO,
        )
        assertEquals(expected, results.mapValues { it.value.status })
    }

    @Test
    fun `DEV-0204 em mWh vira OK depois de converter (sem converter seria CRITICO)`() {
        val dev0204 = devices.single { it.id == "DEV-0204" }
        assertEquals(980.0, dev0204.energyConsumedMah!!, 1e-6)
        assertEquals(OK, results.getValue("DEV-0204").status)
        assertEquals(6.32, results.getValue("DEV-0204").drainPctPerHour!!, 0.01)
    }

    @Test
    fun `DEV-0522 e CRITICO por dreno de 13,4 por hora`() {
        val result = results.getValue("DEV-0522")
        assertEquals(Signal.DRENO, result.reason!!.signal)
        assertEquals(13.4, result.drainPctPerHour!!, 0.01)
    }

    @Test
    fun `divisores zerados viram INDETERMINADO com o sinal certo`() {
        assertEquals(Signal.CAPACIDADE_INVALIDA, results.getValue("DEV-0311").reason!!.signal)
        assertEquals(Signal.TELA_INVALIDA, results.getValue("DEV-0745").reason!!.signal)
    }

    @Test
    fun `nenhum dreno calculado e NaN ou infinito`() {
        results.values.mapNotNull { it.drainPctPerHour }.forEach {
            assertEquals("dreno inválido: $it", true, it.isFinite())
        }
    }
}
