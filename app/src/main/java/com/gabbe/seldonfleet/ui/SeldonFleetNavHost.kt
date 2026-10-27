package com.gabbe.seldonfleet.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gabbe.seldonfleet.ui.detail.DetailScreen
import com.gabbe.seldonfleet.ui.fleet.FleetScreen
import kotlinx.serialization.Serializable

// Rotas tipadas: o compilador garante que o detalhe sempre recebe um deviceId.
@Serializable
object FleetRoute

@Serializable
data class DetailRoute(val deviceId: String)

@Composable
fun SeldonFleetNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = FleetRoute) {
        // O FleetViewModel pertence a esta entrada da pilha: continua vivo enquanto o
        // detalhe está aberto, e por isso o filtro reaparece igual ao voltar.
        composable<FleetRoute> {
            FleetScreen(onDeviceClick = { id -> navController.navigate(DetailRoute(id)) })
        }
        composable<DetailRoute> {
            DetailScreen(
                onBack = {
                    // Evita esvaziar a pilha com dois toques rápidos na seta.
                    if (navController.previousBackStackEntry != null) navController.popBackStack()
                },
            )
        }
    }
}
