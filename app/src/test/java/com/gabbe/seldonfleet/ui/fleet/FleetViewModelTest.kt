package com.gabbe.seldonfleet.ui.fleet

import androidx.lifecycle.SavedStateHandle
import com.gabbe.seldonfleet.domain.ClassifyDevice
import com.gabbe.seldonfleet.domain.Device
import com.gabbe.seldonfleet.domain.DeviceRepository
import com.gabbe.seldonfleet.domain.DeviceStatus
import com.gabbe.seldonfleet.domain.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Fakes: implementam as interfaces do domínio sem arquivo nem DataStore.
// Nenhuma linha de domain/ precisou mudar para trocar a implementação.
private class FakeDeviceRepository(private val devices: List<Device>) : DeviceRepository {
    override suspend fun getDevices() = devices
}

private class FailingDeviceRepository : DeviceRepository {
    override suspend fun getDevices(): List<Device> = error("arquivo ilegível")
}

private class FakeFavoritesRepository : FavoritesRepository {
    override val favorites = MutableStateFlow(emptySet<String>())
    override suspend fun toggle(id: String) {
        favorites.value = favorites.value.let { if (id in it) it - id else it + id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FleetViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // 6000 mAh e 60 min: dreno %/h = energia / 60
    private fun device(id: String, energyMah: Double = 0.0, capacityMah: Int = 6000, crashes: Int = 0) = Device(
        id = id, model = "M", soc = null, tier = null, androidApi = null, batteryLevelPct = 50,
        batteryCapacityMah = capacityMah, screenOnMinutes = 60, energyConsumedMah = energyMah,
        jankFramesPct = 0.0, crashCount7d = crashes, lastSeen = null,
    )

    private val fleet = listOf(
        device("OK-1"),
        device("OK-2"),
        device("ATENCAO-1", crashes = 1),
        device("CRITICO-1", energyMah = 780.0),
        device("SEMDADOS-1", capacityMah = 0),
    )

    private fun viewModel(
        repository: DeviceRepository = FakeDeviceRepository(fleet),
        favorites: FavoritesRepository = FakeFavoritesRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = FleetViewModel(repository, favorites, ClassifyDevice(), handle)

    // uiState só é calculado com alguém coletando (WhileSubscribed); o coletor começa na hora.
    private fun TestScope.startCollecting(vm: FleetViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `contagem por status e total`() = runTest {
        val vm = viewModel()
        startCollecting(vm)
        val state = vm.uiState.value
        assertEquals(5, state.total)
        assertEquals(
            mapOf(
                DeviceStatus.OK to 2, DeviceStatus.ATENCAO to 1,
                DeviceStatus.CRITICO to 1, DeviceStatus.INDETERMINADO to 1,
            ),
            state.counts,
        )
        assertEquals(5, state.items.size)
    }

    @Test
    fun `filtro mostra so o status escolhido mas mantem as contagens`() = runTest {
        val vm = viewModel()
        startCollecting(vm)
        vm.selectFilter(DeviceStatus.OK)
        val state = vm.uiState.value
        assertEquals(listOf("OK-1", "OK-2"), state.items.map { it.device.id })
        assertEquals(5, state.total)
        assertEquals(1, state.counts[DeviceStatus.CRITICO])
    }

    @Test
    fun `filtro nulo volta a mostrar todos`() = runTest {
        val vm = viewModel()
        startCollecting(vm)
        vm.selectFilter(DeviceStatus.CRITICO)
        vm.selectFilter(null)
        assertEquals(5, vm.uiState.value.items.size)
    }

    @Test
    fun `filtro e restaurado do SavedStateHandle (rotacao ou processo recriado)`() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle = handle).selectFilter(DeviceStatus.ATENCAO)

        val recreated = viewModel(handle = handle)
        startCollecting(recreated)
        assertEquals(DeviceStatus.ATENCAO, recreated.uiState.value.filter)
        assertEquals(listOf("ATENCAO-1"), recreated.uiState.value.items.map { it.device.id })
    }

    @Test
    fun `favoritar atualiza o item`() = runTest {
        val vm = viewModel()
        startCollecting(vm)
        vm.toggleFavorite("CRITICO-1")
        val favorites = vm.uiState.value.items.filter { it.isFavorite }.map { it.device.id }
        assertEquals(listOf("CRITICO-1"), favorites)
    }

    @Test
    fun `falha na leitura vira estado de erro, sem crash`() = runTest {
        val vm = viewModel(repository = FailingDeviceRepository())
        startCollecting(vm)
        assertTrue(vm.uiState.value.loadFailed)
        assertTrue(vm.uiState.value.items.isEmpty())
    }
}
