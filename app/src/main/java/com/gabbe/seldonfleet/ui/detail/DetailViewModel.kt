package com.gabbe.seldonfleet.ui.detail

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
import com.gabbe.seldonfleet.domain.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(
        val device: Device,
        val classification: Classification,
        val isFavorite: Boolean,
    ) : DetailUiState
}

class DetailViewModel(
    private val deviceRepository: DeviceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val classify: ClassifyDevice,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // A navegação tipada grava o argumento da rota com o nome da propriedade (DetailRoute.deviceId).
    val deviceId: String = checkNotNull(savedStateHandle[KEY_DEVICE_ID])

    private val entry = MutableStateFlow<Pair<Device, Classification>?>(null)
    private val notFound = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> =
        combine(entry, notFound, favoritesRepository.favorites) { entry, notFound, favorites ->
            when {
                entry != null -> DetailUiState.Content(entry.first, entry.second, deviceId in favorites)
                notFound -> DetailUiState.NotFound
                else -> DetailUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    init {
        // Relê o repositório em vez de receber o objeto pela rota: a rota carrega só o id,
        // e o detalhe continua funcionando se o processo for recriado.
        viewModelScope.launch {
            val device = try {
                deviceRepository.getDevices().firstOrNull { it.id == deviceId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (device == null) notFound.value = true else entry.value = device to classify(device)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { favoritesRepository.toggle(deviceId) }
    }

    companion object {
        const val KEY_DEVICE_ID = "deviceId"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SeldonFleetApp).container
                DetailViewModel(
                    container.deviceRepository,
                    container.favoritesRepository,
                    container.classifyDevice,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
