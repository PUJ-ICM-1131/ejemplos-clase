package com.icm2610.contactos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import com.icm2610.contactos.ui.contacts.ContactsScreen
import com.icm2610.contactos.ui.theme.ContactosTheme

/**
 * ACTIVITY PRINCIPAL
 * ══════════════════════════════════════════════════════════════════════
 *
 * En una app con Jetpack Compose, la Activity es el punto de entrada
 * mínimo. Su única responsabilidad es:
 *  1. Configurar enableEdgeToEdge() → dibuja bajo la barra de estado
 *  2. Llamar a setContent { } con el árbol de composables raíz
 *
 * Toda la lógica de UI, navegación y datos se delega a Composables
 * y ViewModels, manteniendo la Activity lo más delgada posible.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactosTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        /*
                         * TopAppBar: barra superior de Material 3.
                         * pinnedScrollBehavior → permanece visible al hacer scroll.
                         * Para un comportamiento que colapsa al scroll, usar
                         * enterAlwaysScrollBehavior() o exitUntilCollapsedScrollBehavior().
                         */
                        TopAppBar(
                            title = {
                                Text(text = "Mis Contactos")
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    /*
                     * innerPadding: espaciado que provee el Scaffold para que
                     * el contenido no quede debajo de la TopAppBar ni de la
                     * BottomNavigationBar (si la hubiese).
                     * Siempre debe aplicarse con Modifier.padding(innerPadding).
                     */
                    ContactsScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
