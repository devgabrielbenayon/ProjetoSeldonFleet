package com.gabbe.seldonfleet.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {

    // ---------- Classe do aparelho a partir da RAM ----------

    @Test
    fun `4 GB nominais (3,7 GB reportados) e aparelho fraco`() =
        assertEquals(DeviceClass.LOW_END, deviceClassFor(totalRamMb = 3_700, isLowRamDevice = false))

    @Test
    fun `6 GB nominais (5,6 GB reportados) e aparelho padrao`() =
        assertEquals(DeviceClass.STANDARD, deviceClassFor(totalRamMb = 5_600, isLowRamDevice = false))

    @Test
    fun `fronteira de 5 GB`() {
        assertEquals(DeviceClass.LOW_END, deviceClassFor(totalRamMb = 5 * 1024L - 1, isLowRamDevice = false))
        assertEquals(DeviceClass.STANDARD, deviceClassFor(totalRamMb = 5 * 1024L, isLowRamDevice = false))
    }

    @Test
    fun `aparelho marcado pelo sistema como low RAM e fraco mesmo com muita memoria`() =
        assertEquals(DeviceClass.LOW_END, deviceClassFor(totalRamMb = 8_000, isLowRamDevice = true))

    // ---------- Política ----------

    @Test
    fun `nenhuma politica roda perto do minimo de 15 minutos do WorkManager`() =
        DeviceClass.entries.forEach {
            assertTrue("$it roda com frequência demais", refreshPolicyFor(it).intervalHours >= 6)
        }

    @Test
    fun `toda politica exige bateria nao baixa`() =
        DeviceClass.entries.forEach { assertTrue(refreshPolicyFor(it).requiresBatteryNotLow) }

    @Test
    fun `aparelho fraco so atualiza carregando`() =
        assertTrue(refreshPolicyFor(DeviceClass.LOW_END).requiresCharging)

    @Test
    fun `aparelho fraco atualiza com frequencia menor ou igual a do padrao`() =
        assertTrue(
            refreshPolicyFor(DeviceClass.LOW_END).intervalHours >=
                refreshPolicyFor(DeviceClass.STANDARD).intervalHours,
        )

    @Test
    fun `valores atuais da politica`() {
        assertEquals(RefreshPolicy(24, requiresBatteryNotLow = true, requiresCharging = true), refreshPolicyFor(DeviceClass.LOW_END))
        assertEquals(RefreshPolicy(12, requiresBatteryNotLow = true, requiresCharging = false), refreshPolicyFor(DeviceClass.STANDARD))
    }
}
