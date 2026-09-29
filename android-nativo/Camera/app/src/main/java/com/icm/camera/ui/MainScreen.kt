package com.icm.camera.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Pantalla de bienvenida que Navigation 3 muestra como destino inicial.
 *
 * Esta pantalla no mantiene estado ni realiza acciones. Su propósito es ofrecer un contenido
 * neutral mientras el usuario elige una funcionalidad desde el menú lateral.
 */
@Composable
fun MainScreen() {
    // Column organiza sus elementos verticalmente. fillMaxSize hace que tome toda el área que
    // NavDisplay le asigna; los dos parámetros de alineación centran el icono en esa área.
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Se reutiliza un icono de cámara incluido en Android. painterResource lo convierte en
        // un Painter que Compose puede dibujar y size establece un tamaño visible de 150 dp.
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_camera),
            contentDescription = "Icono de cámara",
            modifier = Modifier.size(150.dp)
        )
    }
}
