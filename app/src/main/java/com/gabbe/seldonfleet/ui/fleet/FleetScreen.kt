package com.gabbe.seldonfleet.ui.fleet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabbe.seldonfleet.domain.DeviceStatus
import com.gabbe.seldonfleet.ui.FavoriteButton
import com.gabbe.seldonfleet.ui.StatusChip
import com.gabbe.seldonfleet.ui.formatPt
import com.gabbe.seldonfleet.ui.label

@Composable
fun FleetScreen(
    onDeviceClick: (String) -> Unit,
    viewModel: FleetViewModel = viewModel(factory = FleetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FleetContent(
        state = state,
        onFilterSelected = viewModel::selectFilter,
        onToggleFavorite = viewModel::toggleFavorite,
        onDeviceClick = onDeviceClick,
    )
}

// Sem estado próprio: só desenha o FleetUiState. Nenhuma regra de negócio aqui.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FleetContent(
    state: FleetUiState,
    onFilterSelected: (DeviceStatus?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeviceClick: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Frota") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> Centered { CircularProgressIndicator() }
                state.loadFailed -> Centered { Text("Não foi possível ler a frota.") }
                else -> {
                    FilterRow(state, onFilterSelected)
                    if (state.items.isEmpty()) {
                        Centered { Text("Nenhum aparelho com este status.") }
                    } else {
                        LazyColumn {
                            items(state.items, key = { it.device.id }) { item ->
                                DeviceRow(item, onToggleFavorite, onDeviceClick)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: FleetUiState, onFilterSelected: (DeviceStatus?) -> Unit) {
    val options: List<Pair<DeviceStatus?, String>> =
        listOf(null to "Todos ${state.total}") +
            DeviceStatus.entries.map { it to "${it.label()} ${state.counts[it] ?: 0}" }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.first?.name ?: "TODOS" }) { (status, text) ->
            FilterChip(
                selected = state.filter == status,
                onClick = { onFilterSelected(status) },
                label = { Text(text) },
            )
        }
    }
}

@Composable
private fun DeviceRow(
    item: FleetItem,
    onToggleFavorite: (String) -> Unit,
    onDeviceClick: (String) -> Unit,
) {
    val device = item.device
    ListItem(
        headlineContent = { Text(device.id, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text("${device.model ?: "Modelo desconhecido"} · Bateria ${device.batteryLevelPct.formatPt()}%")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(item.classification.status)
                FavoriteButton(item.isFavorite) { onToggleFavorite(device.id) }
            }
        },
        modifier = Modifier.clickable { onDeviceClick(device.id) },
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        content()
    }
}
