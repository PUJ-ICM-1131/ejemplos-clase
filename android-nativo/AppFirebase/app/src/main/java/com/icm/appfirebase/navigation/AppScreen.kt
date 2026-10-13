package com.icm.appfirebase.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppScreen : NavKey {
    @Serializable data object Login : AppScreen
    @Serializable data object Register : AppScreen
    @Serializable data object Home : AppScreen
    @Serializable data object Chat : AppScreen
}
