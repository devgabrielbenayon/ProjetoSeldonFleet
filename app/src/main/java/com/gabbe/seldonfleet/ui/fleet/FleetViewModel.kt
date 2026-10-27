package com.gabbe.seldonfleet.ui.fleet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gabbe.seldonfleet.SeldonFleetApp
import com.gabbe.seldonfleet.domain.Classification
import com.gabbe.seldonfleet.domain.ClassifyDevice
import com.gabbe.seldonfleet.domain.Device
import com.gabbe.seldonfleet.domain.DeviceRepository
import com.gabbe.seldonfleet.domain.DeviceStatus
import com.gabbe.seldonfleet.domain.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FleetItem(
    val device: Device,
    val classification: Classification,
    val isFavorite: Boolean,
)

data class FleetUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val filter: DeviceStatus? = null,          // null = todos
    val total: Int = 0,
    val counts: Map<DeviceStatus, Int> = emptyMap(),
    val items: List<FleetItem> = emptyList(),  // já filtrados
)

class FleetViewModel(
    private val deviceRepository: DeviceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val classify: ClassifyDevice,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data object Failed : Load
        data class Loaded(val entries: List<Pair<Device, Classification>>) : Load
    }

    private val load = MutableStateFlow<Load>(Load.Loading)

    // No SavedStateHandle o filtro sobrevive à rotação e à morte do processo;
    // ida e volta do detalhe já é coberta porque este ViewModel fica vivo na pilha de navegação.
    private val filter = savedStateHandle.getStateFlow<DeviceStatus?>(KEY_FILTER, null)

    val uiState: StateFlow<FleetUiState> =
        combine(load, favoritesRepository.favorites, filter) { load, favorites, filter ->
            when (load) {
                Load.Loading -> FleetUiState(isLoading = true, filter = filter)
                Load.Failed -> FleetUiState(isLoading = false, loadFailed = true, filter = filter)
                is Load.Loaded -> {
                    val all = load.entries.map { (device, classification) ->
                        FleetItem(device, classification, device.id in favorites)
                    }
                    FleetUiState(
                        isLoading = false,
                        filter = filter,
                        total = all.size,
                        counts = all.groupingBy { it.classification.status }.eachCount(),
                        items = if (filter == null) all else all.filter { it.classification.status == filter },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FleetUiState())

    init {
        // A classificação acontece uma vez, aqui no ViewModel — nunca dentro de um Composable.
        viewModelScope.launch {
            load.value = try {
                Load.Loaded(deviceRepository.getDevices().map { it to classify(it) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed
            }
        }
    }

    fun selectFilter(status: DeviceStatus?) {
        savedStateHandle[KEY_FILTER] = status
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { favoritesRepository.toggle(id) }
    }

    companion object {
        const val KEY_FILTER = "filter"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SeldonFleetApp).container
                FleetViewModel(
                    container.deviceRepository,
                    container.favoritesRepository,
                    container.classifyDevice,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
