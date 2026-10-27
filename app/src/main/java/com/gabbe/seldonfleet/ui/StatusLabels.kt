package com.gabbe.seldonfleet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabbe.seldonfleet.domain.DeviceStatus

// Texto e cor de cada estado moram só na UI; o domínio não sabe o que é "Sem dados".

fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.OK -> "OK"
    DeviceStatus.ATENCAO -> "Atenção"
    DeviceStatus.CRITICO -> "Crítico"
    DeviceStatus.INDETERMINADO -> "Sem dados"
}

// Tons médios: legíveis tanto no tema claro quanto no escuro.
fun DeviceStatus.color(): Color = when (this) {
    DeviceStatus.OK -> Color(0xFF43A047)
    DeviceStatus.ATENCAO -> Color(0xFFF9A825)
    DeviceStatus.CRITICO -> Color(0xFFE53935)
    DeviceStatus.INDETERMINADO -> Color(0xFF78909C)
}

@Composable
fun StatusChip(status: DeviceStatus, modifier: Modifier = Modifier) {
    Surface(
        color = status.color().copy(alpha = 0.18f),
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = status.label(),
            color = status.color(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.semantics {
            contentDescription = if (isFavorite) "Remover dos favoritos" else "Favoritar"
        },
    ) {
        Text(
            text = if (isFavorite) "★" else "☆",
            fontSize = 24.sp,
            color = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
