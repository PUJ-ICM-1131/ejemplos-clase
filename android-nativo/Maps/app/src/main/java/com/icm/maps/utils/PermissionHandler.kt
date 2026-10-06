package com.icm.maps.utils

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(onPermissionChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    // La ubicación aproximada también permite usar el mapa.
    val hasLocationPermission = permissions.permissions.any { it.status.isGranted }
    val currentOnPermissionChanged by rememberUpdatedState(onPermissionChanged)
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasLocationPermission) {
        currentOnPermissionChanged(hasLocationPermission)
    }

    if (!hasLocationPermission) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (permissions.shouldShowRationale) {
                    "La ubicación permite mostrar tu posición y registrar tu recorrido. Puedes permitir ubicación aproximada."
                } else {
                    "Permite el acceso a tu ubicación para ver tu posición en el mapa."
                }
            )
            Button(onClick = {
                hasRequestedPermission = true
                permissions.launchMultiplePermissionRequest()
            }) {
                Text(if (hasRequestedPermission) "Volver a solicitar" else "Permitir ubicación")
            }
            // Accompanist no distingue una primera solicitud de una denegación permanente.
            // Ofrecemos Ajustes tras solicitar, sin afirmar que la denegación sea permanente.
            if (hasRequestedPermission && !permissions.shouldShowRationale) {
                Text("Si no aparece la solicitud, puedes habilitar el permiso en Ajustes.")
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) {
                    Text("Abrir ajustes")
                }
            }
        }
    }
}
