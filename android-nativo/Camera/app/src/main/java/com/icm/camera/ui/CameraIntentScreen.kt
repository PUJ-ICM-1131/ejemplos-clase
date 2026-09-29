package com.icm.camera.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.icm.camera.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla que delega la captura a una aplicación de cámara instalada en el dispositivo.
 *
 * A diferencia de CameraX, esta implementación no controla directamente el sensor ni dibuja
 * una vista previa. ActivityResultContracts abre aplicaciones del sistema para tomar o elegir
 * una imagen y entrega el resultado de vuelta a este composable.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraIntentScreen() {
    // LocalContext entrega el Context asociado a la composición actual. Se usa para consultar
    // permisos, crear archivos y construir la autoridad del FileProvider.
    val context = LocalContext.current

    // URI de la imagen que la interfaz debe mostrar. Puede venir del selector de fotografías o
    // del archivo que recibió la foto tomada por la aplicación de cámara externa.
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // TakePicture devuelve solamente true o false. Por eso se conserva la URI de destino antes
    // de abrir la cámara: si la operación termina bien, esa misma URI pasa a imageUri.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Estos estados controlan los mensajes y las acciones de recuperación que aparecen cuando
    // el permiso fue rechazado o una operación termina con éxito o error.
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // PickVisualMedia abre el selector moderno de fotografías. El contrato devuelve una URI
    // temporal con permiso de lectura; Coil puede consumirla directamente para mostrarla.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { selectedUri ->
            if (selectedUri != null) {
                imageUri = selectedUri
                statusMessage = "Imagen seleccionada"
                Log.d("PhotoPicker", "Imagen seleccionada: $selectedUri")
            } else {
                statusMessage = "No se seleccionó ninguna imagen"
                Log.d("PhotoPicker", "Selección cancelada")
            }
        }
    )

    // TakePicture solicita a otra aplicación que escriba una fotografía en la URI entregada a
    // launch(). El FileProvider permite compartir esa ubicación sin exponer una ruta de archivo.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri = pendingCameraUri
                statusMessage = "Foto guardada en el almacenamiento de la aplicación"
                Log.d("CameraIntent", "Captura completada: $pendingCameraUri")
            } else {
                pendingCameraUri = null
                statusMessage = "La captura se canceló o no pudo completarse"
                Log.e("CameraIntent", "Captura cancelada o fallida")
            }
        }
    )

    /**
     * Prepara un archivo vacío, obtiene su content URI segura y abre la cámara externa.
     * Mantener esta lógica en una función local permite usar el mismo flujo cuando el permiso
     * ya estaba concedido y cuando acaba de ser aceptado por el usuario.
     */
    fun launchExternalCamera() {
        val destinationUri = createImageUri(context)
        pendingCameraUri = destinationUri

        if (destinationUri != null) {
            cameraLauncher.launch(destinationUri)
        } else {
            statusMessage = "No fue posible preparar el archivo de la foto"
            Log.e("CameraIntent", "No se pudo crear la URI de destino")
        }
    }

    // Accompanist mantiene un PermissionState observable para CAMERA. Internamente utiliza el
    // sistema de permisos de Android y actualiza status cuando el usuario responde o vuelve de
    // Ajustes. Si el permiso se concede desde el diálogo, se continúa con la captura pendiente.
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onPermissionResult = { isGranted ->
            cameraPermissionDenied = !isGranted

            if (isGranted) {
                statusMessage = null
                launchExternalCamera()
            } else {
                statusMessage = "Se necesita permiso de cámara para tomar una foto"
                Log.e("CameraIntent", "Permiso de cámara rechazado")
            }
        }
    )

    // Al volver desde Ajustes no se ejecuta onPermissionResult. Este efecto observa el estado
    // de Accompanist y oculta las acciones de recuperación cuando el permiso ya está concedido.
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            cameraPermissionDenied = false
        }
    }

    // La columna ocupa el área del destino y centra verticalmente la vista previa, el mensaje y
    // las acciones. El padding mantiene una separación uniforme respecto de los bordes.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            // AsyncImage usa Coil para abrir la URI fuera del hilo principal. Crop llena el
            // cuadro cuadrado recortando los bordes cuando la proporción de la foto es distinta.
            AsyncImage(
                model = imageUri,
                contentDescription = "Imagen seleccionada o capturada",
                modifier = Modifier
                    .size(250.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            // Hasta recibir una URI válida se presenta una imagen local como marcador visual.
            Image(
                painter = painterResource(id = R.drawable.image_placeholder),
                contentDescription = "No hay imagen seleccionada",
                modifier = Modifier
                    .size(250.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "No hay imagen seleccionada",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // let dibuja el mensaje solamente cuando statusMessage contiene un valor.
        statusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Los dos botones usan weight(1f), de modo que comparten por igual el ancho disponible
        // y se adaptan a pantallas más estrechas sin depender de medidas fijas.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        launchExternalCamera()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Tomar Foto")
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = "Abrir cámara",
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            ElevatedButton(
                onClick = {
                    // ImageOnly limita el selector a imágenes y excluye videos.
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Seleccionar Foto")
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = "Abrir selector de imágenes",
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }

        // Estas acciones aparecen después de un rechazo. Abrir ajustes cubre el caso en el que
        // Android deja de mostrar el diálogo porque el rechazo se volvió permanente.
        if (cameraPermissionDenied) {
            // shouldShowRationale es true cuando Android recomienda explicar por qué se necesita
            // el permiso antes de volver a presentar su diálogo.
            if (cameraPermissionState.status.shouldShowRationale) {
                Text("La cámara es necesaria para tomar una fotografía.")
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        cameraPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Reintentar")
                }
                TextButton(onClick = { openAppSettings(context) }) {
                    Text("Abrir ajustes")
                }
            }
        }
    }
}

/**
 * Crea el archivo donde la aplicación de cámara externa escribirá la fotografía.
 *
 * El archivo se guarda en Pictures dentro del almacenamiento externo específico de la app, por
 * lo que no necesita un permiso general de almacenamiento. FileProvider convierte la ruta real
 * en una content URI que otra aplicación puede utilizar de forma controlada.
 *
 * @return URI compartible del archivo o `null` cuando no existe almacenamiento disponible o se
 * produce un error al crear el archivo.
 */
private fun createImageUri(context: Context): Uri? {
    return try {
        // La fecha y hora reducen la posibilidad de repetir nombres entre capturas.
        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val filePrefix = "JPEG_${timestamp}_"

        // Esta carpeta corresponde a external-files-path/Pictures declarado en file_paths.xml.
        val picturesDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (picturesDirectory == null) {
            Log.e("CameraIntent", "El directorio externo Pictures no está disponible")
            return null
        }

        // createTempFile crea físicamente el destino antes de lanzar la aplicación de cámara.
        val imageFile = File.createTempFile(
            filePrefix,
            ".jpg",
            picturesDirectory
        )

        // La autoridad debe coincidir con ${applicationId}.fileprovider en AndroidManifest.xml.
        val authority = "${context.packageName}.fileprovider"
        FileProvider.getUriForFile(
            context,
            authority,
            imageFile
        )
    } catch (exception: Exception) {
        Log.e("CameraIntent", "Error al crear la URI de la imagen", exception)
        null
    }
}
