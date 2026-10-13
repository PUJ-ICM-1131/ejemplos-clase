package com.icm.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.icm.auth.presentation.auth.AuthViewModel
import com.icm.auth.presentation.auth.LoginScreen
import com.icm.auth.presentation.home.HomeScreen

private data object Login : NavKey
private data object Home : NavKey

@Composable
fun AppNavigation() {
    // Una única instancia compartida entre las pantallas.
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    // La sesión es la fuente de verdad, también al recrear la actividad o el proceso.
    // Cada cambio reemplaza la raíz: Atrás nunca recupera la sesión anterior.
    val destination = if (currentUser != null) Home else Login
    NavDisplay(
        backStack = listOf(destination),
        entryProvider = entryProvider {
            entry<Login> {
                LoginScreen(authViewModel = authViewModel)
            }
            entry<Home> {
                HomeScreen(authViewModel = authViewModel)
            }
        }
    )
}
