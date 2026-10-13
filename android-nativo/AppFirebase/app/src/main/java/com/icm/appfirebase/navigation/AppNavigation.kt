package com.icm.appfirebase.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.icm.appfirebase.presentation.auth.AuthViewModel
import com.icm.appfirebase.presentation.auth.LoginScreen
import com.icm.appfirebase.presentation.auth.RegisterScreen
import com.icm.appfirebase.presentation.chat.ChatScreen
import com.icm.appfirebase.presentation.home.HomeScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(
        if (currentUser == null) AppScreen.Login else AppScreen.Home
    )
    val replaceRoot: (AppScreen) -> Unit = { screen ->
        backStack.clear()
        backStack.add(screen)
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    // No abandonar el registro al crear la cuenta: todavía se guarda el perfil.
    LaunchedEffect(currentUser) {
        if (currentUser == null && backStack.any { it == AppScreen.Home || it == AppScreen.Chat }) {
            replaceRoot(AppScreen.Login)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<AppScreen.Login> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = { replaceRoot(AppScreen.Home) },
                    onNavigateToRegister = {
                        if (backStack.lastOrNull() == AppScreen.Login) backStack.add(AppScreen.Register)
                    }
                )
            }
            entry<AppScreen.Register> {
                RegisterScreen(
                    onRegisterSuccess = { replaceRoot(AppScreen.Home) },
                    onNavigateBack = goBack
                )
            }
            entry<AppScreen.Home> {
                HomeScreen(
                    authViewModel = authViewModel,
                    onLogout = { replaceRoot(AppScreen.Login) },
                    onNavigateToChat = {
                        if (backStack.lastOrNull() == AppScreen.Home) backStack.add(AppScreen.Chat)
                    }
                )
            }
            entry<AppScreen.Chat> {
                ChatScreen(onNavigateBack = goBack)
            }
        }
    )
}
