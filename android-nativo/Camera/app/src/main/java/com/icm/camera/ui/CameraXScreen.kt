package com.icm.camera.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.icm.camera.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * Pantalla de cámara integrada construida con CameraX.
 *
 * CameraX controla el sensor dentro de la aplicación: conecta una vista previa y un caso de uso
 * de captura al ciclo de vida de esta pantalla. También administra los permisos necesarios y
 * guarda cada fotografía directamente en la colección pública de imágenes de MediaStore.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXScreen() {
    // Context permite acceder a permisos, MediaStore y al proveedor de CameraX.
    val context = LocalContext.current

    // CameraX se enlaza a este LifecycleOwner. Al salir de la pantalla, la librería pausa o
    // libera la cámara de acuerdo con el estado del ciclo de vida.
    val lifecycleOwner = LocalLifecycleOwner.current

    // Los callbacks de CameraX se reciben en el executor principal para que puedan actualizar
    // estados de Compose de forma segura.
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    // Este indicador evita pedir automáticamente el permiso más de una vez. Accompanist guarda
    // por separado el estado real del permiso y lo actualiza cuando cambia el ciclo de vida.
    var cameraPermissionRequested by rememberSaveable { mutableStateOf(false) }

    // Mensaje informativo que se actualiza al iniciar la cámara o guardar una fotografía.
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Accompanist envuelve Activity Result API en un PermissionState observable por Compose.
    // Cuando el usuario responde, status cambia y provoca la recomposición de esta pantalla.
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        onPermissionResult = {
            cameraPermissionRequested = true
        }
    )

    // LaunchedEffect(Unit) se ejecuta una sola vez mientras esta instancia permanezca en la
    // composición. Solicita el permiso al entrar por primera vez, si todavía hace falta.
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted && !cameraPermissionRequested) {
            cameraPermissionRequested = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Sin permiso no se intenta abrir el hardware. El return hace que esta interfaz de
    // recuperación sea el único contenido de la pantalla hasta que el permiso sea concedido.
    if (!cameraPermissionState.status.isGranted) {
        // shouldShowRationale indica que Android permite explicar el motivo antes de repetir la
        // solicitud. Accompanist no puede distinguir el primer intento de un rechazo permanente,
        // por eso la interfaz ofrece también abrir Ajustes en ambos casos.
        val permissionMessage = if (cameraPermissionState.status.shouldShowRationale) {
            "La cámara es necesaria para mostrar la vista previa y tomar fotografías."
        } else {
            "Se necesita permiso de cámara para mostrar la vista previa."
        }

        PermissionRequiredContent(
            message = permissionMessage,
            onRequestPermission = {
                cameraPermissionState.launchPermissionRequest()
            },
            onOpenSettings = { openAppSettings(context) }
        )
        return
    }

    // PreviewView es una View tradicional optimizada para recibir los fotogramas de CameraX.
    // remember evita reconstruirla en cada recomposición.
    val previewView = remember { PreviewView(context) }

    // ImageCapture representa el caso de uso encargado de producir archivos JPEG al presionar
    // el botón. Se crea una sola vez para vincular la misma instancia al CameraProvider.
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Android 8.1 y versiones anteriores requieren permiso de escritura para guardar en la
    // galería pública. Este estado controla la ayuda que se muestra después de un rechazo.
    var storagePermissionDenied by rememberSaveable { mutableStateOf(false) }

    // Un segundo PermissionState administra WRITE_EXTERNAL_STORAGE. Aunque existe en todas las
    // versiones, solamente se consulta y solicita cuando el dispositivo ejecuta API 28 o menor.
    val storagePermissionState = rememberPermissionState(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        onPermissionResult = { granted ->
            storagePermissionDenied = !granted
            if (granted) {
                capturePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    executor = cameraExecutor,
                    onResult = { statusMessage = it }
                )
            } else {
                statusMessage =
                    "Se necesita permiso de almacenamiento en Android 8.1 o anterior"
            }
        }
    )

    // Este efecto obtiene el ProcessCameraProvider y vincula sus casos de uso una vez que la
    // PreviewView y el LifecycleOwner están disponibles.
    LaunchedEffect(previewView, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        // ProcessCameraProvider se obtiene de forma asíncrona. El listener se ejecuta en el
        // executor principal cuando la inicialización termina.
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview produce los fotogramas y surfaceProvider los dirige a PreviewView.
                val preview = androidx.camera.core.Preview.Builder()
                    .build()
                    .also { cameraPreview ->
                        cameraPreview.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Se prefiere la cámara trasera. Si no existe, se usa la frontal; esto permite
                // ejecutar la app en dispositivos que solo cuentan con una cámara.
                val cameraSelector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }

                    else -> {
                        statusMessage = "Este dispositivo no tiene una cámara disponible"
                        return@addListener
                    }
                }

                // Se eliminan enlaces anteriores para no conectar dos veces los mismos casos de
                // uso. Después se asocian Preview e ImageCapture al ciclo de vida de la pantalla.
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exception: Exception) {
                statusMessage = "No fue posible iniciar la cámara"
                Log.e("CameraXScreen", "Error al inicializar CameraX", exception)
            }
        }, cameraExecutor)
    }

    // Box permite superponer la vista previa, los mensajes y el botón de captura.
    Box(modifier = Modifier.fillMaxSize()) {
        // AndroidView integra PreviewView, que pertenece al sistema clásico de Views, dentro del
        // árbol de Compose. La vista ocupa todo el espacio disponible.
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Los resultados de inicialización y captura aparecen sobre la parte superior del visor.
        statusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // Esta ayuda solo puede aparecer en Android 8.1 o anterior. Permite repetir la solicitud
        // o conceder el permiso manualmente desde la ficha de ajustes de la aplicación.
        if (storagePermissionDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Se necesita permiso de almacenamiento para guardar la foto.")
                TextButton(
                    onClick = {
                        storagePermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Reintentar")
                }
                TextButton(onClick = { openAppSettings(context) }) {
                    Text("Abrir ajustes")
                }
            }
        }

        // El botón flotante se mantiene centrado en la parte inferior del visor. Antes de tomar
        // la foto comprueba el permiso heredado que únicamente requieren las API 24 a 28.
        FloatingActionButton(
            onClick = {
                val needsLegacyStoragePermission =
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                        !storagePermissionState.status.isGranted

                if (needsLegacyStoragePermission) {
                    storagePermissionState.launchPermissionRequest()
                } else {
                    storagePermissionDenied = false
                    capturePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        executor = cameraExecutor,
                        onResult = { statusMessage = it }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(56.dp),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = "Tomar foto"
            )
        }
    }
}

/**
 * Captura una fotografía y pide a CameraX que la escriba directamente en MediaStore.
 *
 * Guardar mediante ContentResolver evita crear un Bitmap completo en memoria. También permite
 * que la imagen aparezca en la galería pública y que Android administre su ubicación física.
 *
 * @param context proporciona el ContentResolver utilizado para acceder a MediaStore.
 * @param imageCapture caso de uso de CameraX vinculado actualmente a la cámara.
 * @param executor hilo donde CameraX entregará el callback final.
 * @param onResult comunica a Compose un mensaje de éxito o error para mostrarlo en pantalla.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onResult: (String) -> Unit
) {
    // Un nombre basado en fecha y hora facilita reconocer la imagen y evita sobrescribir otra.
    val displayName = "Photo_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }"

    // ContentValues describe la nueva entrada de MediaStore. Desde Android 10, RELATIVE_PATH
    // permite elegir Pictures/Camera sin solicitar acceso general al almacenamiento.
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Camera"
            )
        }
    }

    // OutputFileOptions conecta ImageCapture con la colección pública de imágenes. CameraX se
    // ocupa de insertar la entrada y escribir en ella los bytes JPEG producidos por el sensor.
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult("Foto guardada en la galería")
                Log.d("CameraXScreen", "Imagen guardada: ${output.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {
                onResult("No fue posible guardar la foto")
                Log.e("CameraXScreen", "Error al capturar la foto", exception)
            }
        }
    )
}
