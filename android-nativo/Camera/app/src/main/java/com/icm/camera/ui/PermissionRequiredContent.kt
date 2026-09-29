package com.icm.camera.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Interfaz reutilizable que se muestra cuando una función necesita un permiso no concedido.
 *
 * La pantalla no solicita permisos directamente. Recibe dos callbacks para que la pantalla que
 * la utiliza decida qué permiso pedir y cómo reaccionar. Esta separación permite reutilizar el
 * mismo diseño para cámara, almacenamiento u otros permisos.
 *
 * @param message explicación visible de por qué la aplicación necesita el permiso.
 * @param onRequestPermission acción para volver a lanzar el diálogo de permiso de Android.
 * @param onOpenSettings acción para abrir los ajustes cuando Android ya no muestra el diálogo,
 * por ejemplo después de que el usuario selecciona "No volver a preguntar".
 */
@Composable
fun PermissionRequiredContent(
    message: String,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // La columna ocupa la pantalla completa y centra vertical y horizontalmente el mensaje y
    // sus acciones. El padding evita que el contenido toque los bordes en pantallas pequeñas.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message)

        // Este botón ejecuta el launcher de permisos proporcionado por la pantalla llamadora.
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Solicitar permiso")
        }

        // Abrir ajustes es la vía de recuperación cuando el permiso fue rechazado de forma
        // permanente y el sistema operativo deja de presentar su diálogo.
        TextButton(onClick = onOpenSettings) {
            Text("Abrir ajustes")
        }
    }
}

/**
 * Abre la ficha de esta aplicación dentro de los ajustes de Android.
 *
 * ACTION_APPLICATION_DETAILS_SETTINGS necesita una URI con el esquema "package" para saber
 * qué aplicación debe mostrar. FLAG_ACTIVITY_NEW_TASK permite iniciar la pantalla usando un
 * [Context] que no necesariamente sea una Activity.
 */
fun openAppSettings(context: Context) {
    val applicationUri = Uri.fromParts(
        "package",
        context.packageName,
        null
    )
    val settingsIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        applicationUri
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(settingsIntent)
}
