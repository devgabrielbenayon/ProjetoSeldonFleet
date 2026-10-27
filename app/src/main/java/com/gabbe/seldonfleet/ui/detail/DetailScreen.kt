package com.gabbe.seldonfleet.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabbe.seldonfleet.ui.FavoriteButton
import com.gabbe.seldonfleet.ui.StatusChip
import com.gabbe.seldonfleet.ui.formatLastSeen
import com.gabbe.seldonfleet.ui.formatPt
import com.gabbe.seldonfleet.ui.reasonText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.deviceId) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Voltar" }) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    (state as? DetailUiState.Content)?.let {
                        FavoriteButton(it.isFavorite, viewModel::toggleFavorite)
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                DetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailUiState.NotFound ->
                    Text("Aparelho não encontrado.", Modifier.align(Alignment.Center))
                is DetailUiState.Content -> DetailContent(s)
            }
        }
    }
}

@Composable
private fun DetailContent(state: DetailUiState.Content) {
    val device = state.device
    val classification = state.classification
    val converted = device.energyReportedUnit == "mWh" && device.energyConsumedMah != null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip(classification.status)
            Text(device.model ?: "Modelo desconhecido", style = MaterialTheme.typography.titleMedium)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Por que esta classificação", style = MaterialTheme.typography.titleSmall)
                Text(reasonText(classification, device), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Column {
            Attribute("Dreno relativo", "${classification.drainPctPerHour.formatPt()} %/h")
            Attribute("Consumo por hora", "${classification.drainMahPerHour.formatPt()} mAh/h")
            Attribute(
                "Consumo total" + if (converted) " (convertido de mWh)" else "",
                "${device.energyConsumedMah.formatPt()} mAh",
            )
            Attribute("Capacidade da bateria", "${device.batteryCapacityMah.formatPt()} mAh")
            Attribute("Nível de bateria", "${device.batteryLevelPct.formatPt()} %")
            Attribute("Tela ligada", "${device.screenOnMinutes.formatPt()} min")
            Attribute("Quadros perdidos", "${device.jankFramesPct.formatPt()} %")
            Attribute("Falhas (7 dias)", device.crashCount7d.formatPt())
            Attribute("SoC", device.soc ?: "—")
            Attribute("Categoria", device.tier ?: "—")
            Attribute("Nível de API", device.androidApi.formatPt())
            Attribute("Último contato", formatLastSeen(device.lastSeen))
        }
    }
}

@Composable
private fun Attribute(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
    HorizontalDivider()
}
