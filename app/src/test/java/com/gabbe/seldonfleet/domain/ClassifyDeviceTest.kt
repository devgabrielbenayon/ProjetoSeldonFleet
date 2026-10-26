package com.gabbe.seldonfleet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassifyDeviceTest {

    private val classify = ClassifyDevice()

    // Capacidade 6000 mAh e 60 min de tela: dreno relativo (%/h) = energia / 60.
    // Ex.: 720 mAh → 12,0 %/h. Facilita montar valores exatamente na fronteira.
    private fun device(
        energyMah: Double? = 0.0,
        capacityMah: Int? = 6000,
        screenOnMinutes: Int? = 60,
        crashes: Int? = 0,
        jankPct: Double? = 0.0,
    ) = Device(
        id = "DEV-TEST",
        model = "Teste",
        soc = "SoC",
        tier = "mid",
        androidApi = 34,
        batteryLevelPct = 50,
        batteryCapacityMah = capacityMah,
        screenOnMinutes = screenOnMinutes,
        energyConsumedMah = energyMah,
        jankFramesPct = jankPct,
        crashCount7d = crashes,
        lastSeen = "2026-10-20T00:00:00Z",
    )

    private fun assertClassified(
        result: Classification,
        status: DeviceStatus,
        signal: Signal?,
    ) {
        assertEquals(status, result.status)
        assertEquals(signal, result.reason?.signal)
    }

    // ---------- Fronteiras do dreno relativo ----------

    @Test
    fun `dreno 11,99 fica em ATENCAO`() =
        assertClassified(classify(device(energyMah = 719.4)), DeviceStatus.ATENCAO, Signal.DRENO)

    @Test
    fun `dreno exatamente 12,0 vira CRITICO`() {
        val result = classify(device(energyMah = 720.0))
        assertClassified(result, DeviceStatus.CRITICO, Signal.DRENO)
        assertEquals(12.0, result.reason!!.observed!!, 1e-9)
        assertEquals(12.0, result.reason!!.threshold!!, 1e-9)
    }

    @Test
    fun `dreno 6,99 fica OK`() =
        assertClassified(classify(device(energyMah = 419.4)), DeviceStatus.OK, null)

    @Test
    fun `dreno exatamente 7,0 vira ATENCAO`() =
        assertClassified(classify(device(energyMah = 420.0)), DeviceStatus.ATENCAO, Signal.DRENO)

    // ---------- Fronteiras de falhas ----------

    @Test
    fun `2 falhas ficam em ATENCAO`() =
        assertClassified(classify(device(crashes = 2)), DeviceStatus.ATENCAO, Signal.FALHAS)

    @Test
    fun `3 falhas viram CRITICO`() {
        val result = classify(device(crashes = 3))
        assertClassified(result, DeviceStatus.CRITICO, Signal.FALHAS)
        assertEquals(3.0, result.reason!!.observed!!, 1e-9)
    }

    @Test
    fun `0 falhas fica OK`() =
        assertClassified(classify(device(crashes = 0)), DeviceStatus.OK, null)

    @Test
    fun `1 falha vira ATENCAO`() =
        assertClassified(classify(device(crashes = 1)), DeviceStatus.ATENCAO, Signal.FALHAS)

    // ---------- Fronteiras de quadros perdidos ----------

    @Test
    fun `quadros 14,99 ficam em ATENCAO`() =
        assertClassified(classify(device(jankPct = 14.99)), DeviceStatus.ATENCAO, Signal.QUADROS_PERDIDOS)

    @Test
    fun `quadros exatamente 15,0 viram CRITICO`() =
        assertClassified(classify(device(jankPct = 15.0)), DeviceStatus.CRITICO, Signal.QUADROS_PERDIDOS)

    @Test
    fun `quadros 7,99 ficam OK`() =
        assertClassified(classify(device(jankPct = 7.99)), DeviceStatus.OK, null)

    @Test
    fun `quadros exatamente 8,0 viram ATENCAO`() =
        assertClassified(classify(device(jankPct = 8.0)), DeviceStatus.ATENCAO, Signal.QUADROS_PERDIDOS)

    // ---------- Registros incompletos → INDETERMINADO, sem exceção e sem NaN ----------

    private fun assertIndeterminate(result: Classification, signal: Signal) {
        assertClassified(result, DeviceStatus.INDETERMINADO, signal)
        assertNull(result.drainPctPerHour)
        assertNull(result.drainMahPerHour)
        assertNull(result.reason!!.threshold)
    }

    @Test
    fun `capacidade 0 e INDETERMINADO`() {
        val result = classify(device(capacityMah = 0))
        assertIndeterminate(result, Signal.CAPACIDADE_INVALIDA)
        assertEquals(0.0, result.reason!!.observed!!, 1e-9)
    }

    @Test
    fun `capacidade negativa e INDETERMINADO`() =
        assertIndeterminate(classify(device(capacityMah = -100)), Signal.CAPACIDADE_INVALIDA)

    @Test
    fun `capacidade ausente e INDETERMINADO`() {
        val result = classify(device(capacityMah = null))
        assertIndeterminate(result, Signal.CAPACIDADE_INVALIDA)
        assertNull(result.reason!!.observed)
    }

    @Test
    fun `tela 0 e INDETERMINADO`() =
        assertIndeterminate(classify(device(screenOnMinutes = 0)), Signal.TELA_INVALIDA)

    @Test
    fun `tela negativa e INDETERMINADO`() =
        assertIndeterminate(classify(device(screenOnMinutes = -5)), Signal.TELA_INVALIDA)

    @Test
    fun `tela ausente e INDETERMINADO`() =
        assertIndeterminate(classify(device(screenOnMinutes = null)), Signal.TELA_INVALIDA)

    @Test
    fun `energia ausente e INDETERMINADO`() =
        assertIndeterminate(classify(device(energyMah = null)), Signal.ENERGIA_AUSENTE)

    @Test
    fun `falhas ausentes sao INDETERMINADO`() =
        assertIndeterminate(classify(device(crashes = null)), Signal.FALHAS_AUSENTES)

    @Test
    fun `quadros ausentes sao INDETERMINADO`() =
        assertIndeterminate(classify(device(jankPct = null)), Signal.QUADROS_PERDIDOS_AUSENTES)

    @Test
    fun `energia 0 e valida e da dreno 0`() {
        val result = classify(device(energyMah = 0.0))
        assertClassified(result, DeviceStatus.OK, null)
        assertEquals(0.0, result.drainPctPerHour!!, 1e-9)
    }

    // ---------- Precedência ----------

    @Test
    fun `INDETERMINADO vence CRITICO`() =
        assertIndeterminate(classify(device(capacityMah = 0, crashes = 5)), Signal.CAPACIDADE_INVALIDA)

    @Test
    fun `capacidade e checada antes da tela`() =
        assertIndeterminate(classify(device(capacityMah = 0, screenOnMinutes = 0)), Signal.CAPACIDADE_INVALIDA)

    @Test
    fun `dreno critico com quadros de atencao e CRITICO por DRENO`() =
        assertClassified(classify(device(energyMah = 780.0, jankPct = 9.0)), DeviceStatus.CRITICO, Signal.DRENO)

    @Test
    fun `dreno critico e 3 falhas reporta DRENO primeiro`() =
        assertClassified(classify(device(energyMah = 780.0, crashes = 3)), DeviceStatus.CRITICO, Signal.DRENO)

    @Test
    fun `3 falhas e 15 de quadros reporta FALHAS antes de QUADROS`() =
        assertClassified(classify(device(crashes = 3, jankPct = 15.0)), DeviceStatus.CRITICO, Signal.FALHAS)

    @Test
    fun `nivel critico vence nivel de atencao mesmo com sinal posterior`() =
        // dreno 8 %/h (atenção) não pode esconder 3 falhas (crítico)
        assertClassified(classify(device(energyMah = 480.0, crashes = 3)), DeviceStatus.CRITICO, Signal.FALHAS)

    // ---------- Limiares configuráveis ----------

    @Test
    fun `limiares customizados sao respeitados`() {
        val strict = ClassifyDevice(Thresholds(criticalDrain = 5.0))
        assertClassified(strict(device(energyMah = 300.0)), DeviceStatus.CRITICO, Signal.DRENO)
    }

    // ---------- Casos reais do gabarito ----------

    @Test
    fun `DEV-0522 e CRITICO por dreno de 13,4 por hora`() {
        // 6136,5 mWh / 3,87 V: a conversão é da camada de dados; o domínio recebe mAh
        val dev0522 = device(
            energyMah = 6136.5 / 3.87,
            capacityMah = 5000,
            screenOnMinutes = 142,
            crashes = 1,
            jankPct = 9.8,
        )
        val result = classify(dev0522)
        assertClassified(result, DeviceStatus.CRITICO, Signal.DRENO)
        assertEquals(13.4, result.drainPctPerHour!!, 0.01)
        assertEquals(670.0, result.drainMahPerHour!!, 0.5)
    }

    @Test
    fun `DEV-0137 e ATENCAO por falhas`() {
        val dev0137 = device(
            energyMah = 980.0,
            capacityMah = 4410,
            screenOnMinutes = 214,
            crashes = 2,
            jankPct = 7.4,
        )
        val result = classify(dev0137)
        assertClassified(result, DeviceStatus.ATENCAO, Signal.FALHAS)
        assertEquals(6.23, result.drainPctPerHour!!, 0.01)
    }

    @Test
    fun `OK tem reason nulo mas drenos preenchidos`() {
        val result = classify(device(energyMah = 300.0))
        assertClassified(result, DeviceStatus.OK, null)
        assertEquals(5.0, result.drainPctPerHour!!, 1e-9)
        assertEquals(300.0, result.drainMahPerHour!!, 1e-9)
    }
}
