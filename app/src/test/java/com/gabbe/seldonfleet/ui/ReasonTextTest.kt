package com.gabbe.seldonfleet.ui

import com.gabbe.seldonfleet.domain.ClassifyDevice
import com.gabbe.seldonfleet.domain.Device
import org.junit.Assert.assertEquals
import org.junit.Test

class ReasonTextTest {

    private val classify = ClassifyDevice()

    private fun device(
        energyMah: Double? = 0.0,
        unit: String? = "mAh",
        capacityMah: Int? = 6000,
        screenOnMinutes: Int? = 60,
        crashes: Int? = 0,
        jankPct: Double? = 0.0,
    ) = Device(
        id = "DEV-TEST", model = null, soc = null, tier = null, androidApi = null,
        batteryLevelPct = null, batteryCapacityMah = capacityMah, screenOnMinutes = screenOnMinutes,
        energyConsumedMah = energyMah, jankFramesPct = jankPct, crashCount7d = crashes,
        lastSeen = null, energyReportedUnit = unit,
    )

    private fun textFor(device: Device) = reasonText(classify(device), device)

    @Test
    fun `frase do exemplo da especificacao para DEV-0522`() {
        val dev0522 = device(
            energyMah = 6136.5 / 3.87, unit = "mWh", capacityMah = 5000,
            screenOnMinutes = 142, crashes = 1, jankPct = 9.8,
        )
        assertEquals(
            "Dreno relativo de 13,4 %/h, acima do limiar de 12 %/h. " +
                "Consumo normalizado a partir de mWh antes da comparação.",
            textFor(dev0522),
        )
    }

    @Test
    fun `valor exatamente no limiar diz igual ao limiar`() =
        assertEquals(
            "Dreno relativo de 12,0 %/h, igual ao limiar de 12 %/h.",
            textFor(device(energyMah = 720.0)),
        )

    @Test
    fun `falhas citam a contagem e o limiar`() =
        assertEquals(
            "2 falhas nos últimos 7 dias, acima do limiar de 1 falha.",
            textFor(device(crashes = 2)),
        )

    @Test
    fun `quadros perdidos citam o percentual`() =
        assertEquals(
            "Quadros perdidos em 15,0 %, igual ao limiar de 15 %.",
            textFor(device(jankPct = 15.0)),
        )

    @Test
    fun `capacidade zero explica o motivo`() =
        assertEquals(
            "Capacidade da bateria registrada como 0 mAh; não é possível calcular o dreno.",
            textFor(device(capacityMah = 0)),
        )

    @Test
    fun `tela ausente explica o motivo`() =
        assertEquals(
            "Tempo de tela ligada ausente no arquivo; não é possível calcular o dreno.",
            textFor(device(screenOnMinutes = null)),
        )

    @Test
    fun `mWh sem tensao explica que nao foi possivel converter`() =
        assertEquals(
            "Consumo reportado em mWh sem tensão nominal válida; não é possível converter para mAh.",
            textFor(device(energyMah = null, unit = "mWh")),
        )

    @Test
    fun `OK informa o dreno calculado`() =
        assertEquals(
            "Nenhum sinal atingiu os limiares de atenção. Dreno relativo de 5,0 %/h.",
            textFor(device(energyMah = 300.0)),
        )
}
