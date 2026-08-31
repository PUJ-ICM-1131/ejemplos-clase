package com.icm2610.contactos.ui.contacts

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.icm2610.contactos.ui.contacts.components.ContactItem

/**
 * PANTALLA: ContactsScreen
 * ══════════════════════════════════════════════════════════════════════
 *
 * FLUJO DE PERMISOS EN RUNTIME (Android 6.0+ / API 23+)
 * ───────────────────────────────────────────────────────
 * En Android moderno los permisos "peligrosos" tienen 4 estados posibles:
 *
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  ESTADO DEL PERMISO         │ shouldShowRationale │ Acción  │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │ Nunca solicitado (1ra vez)  │       false         │ Pedir   │
 *  │ Denegado (sin "no pedir")   │       true          │ Explicar│
 *  │ Denegado permanentemente    │       false*        │ Settings│
 *  │ Concedido                   │        -            │ Mostrar │
 *  └──────────────────────────────────────────────────────────────┘
 *
 *  (*) shouldShowRationale=false puede ser primera vez O permanentemente
 *      denegado. Para diferenciarlos, rastreamos si ya solicitamos permiso
 *      con `rememberSaveable` (sobrevive rotación de pantalla).
 *
 * ACCOMPANIST PERMISSIONS
 * ────────────────────────
 * `rememberPermissionState(permission)` retorna un objeto con:
 *   • status                    → PermissionStatus.Granted | Denied
 *   • status.shouldShowRationale → si mostrar justificación
 *   • launchPermissionRequest() → dispara el diálogo del sistema
 *
 * @OptIn(ExperimentalPermissionsApi::class) es necesario porque la API
 * todavía está marcada como experimental.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = viewModel()
) {
    // ─── 1. Estado del permiso READ_CONTACTS ──────────────────────────────────
    val contactsPermission = rememberPermissionState(
        permission = Manifest.permission.READ_CONTACTS
    )

    // ─── 2. Estado de la UI y búsqueda desde el ViewModel ────────────────────
    /*
     * collectAsStateWithLifecycle():
     *  • Recolecta el StateFlow como State de Compose.
     *  • A diferencia de collectAsState(), detiene la recolección cuando
     *    la app pasa a segundo plano (lifecycle-aware).
     *  • Resultado: menos batería consumida y trabajo innecesario evitado.
     */
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // ─── 3. Rastrea si ya solicitamos el permiso (para detectar "permanente") ─
    /*
     * rememberSaveable: como `remember`, pero sobrevive cambios de configuración
     * (rotar pantalla) gracias a que guarda el valor en el Bundle.
     */
    var permissionAlreadyRequested by rememberSaveable { mutableStateOf(false) }

    // ─── 4. Cargar contactos cuando el permiso se concede ────────────────────
    /*
     * LaunchedEffect(key): lanza una coroutine cuando el composable
     * entra en composición Y cada vez que `key` cambia.
     * Aquí se ejecuta cuando cambia el estado del permiso.
     */
    LaunchedEffect(contactsPermission.status) {
        if (contactsPermission.status == PermissionStatus.Granted) {
            viewModel.loadContacts()
        }
    }

    // ─── 5. Árbol de decisión según el estado del permiso ────────────────────
    Box(modifier = modifier.fillMaxSize()) {
        when (val status = contactsPermission.status) {

            // ✅ PERMISO CONCEDIDO → Mostrar la lista de contactos
            PermissionStatus.Granted -> {
                ContactsContent(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged
                )
            }

            // ❌ PERMISO DENEGADO → Decidir qué mostrar
            is PermissionStatus.Denied -> {
                when {
                    // Caso 1: El usuario denegó antes → mostrar justificación (rationale)
                    status.shouldShowRationale -> {
                        PermissionRationaleScreen(
                            onRequestPermission = {
                                permissionAlreadyRequested = true
                                contactsPermission.launchPermissionRequest()
                            }
                        )
                    }

                    // Caso 2: Ya solicitamos el permiso pero sigue denegado
                    //         → fue marcado como "No preguntar de nuevo" → ir a Ajustes
                    permissionAlreadyRequested -> {
                        PermissionPermanentlyDeniedScreen()
                    }

                    // Caso 3: Primera vez → solicitar el permiso directamente
                    else -> {
                        PermissionRequestScreen(
                            onRequestPermission = {
                                permissionAlreadyRequested = true
                                contactsPermission.launchPermissionRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPOSABLES INTERNOS
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Contenido principal: barra de búsqueda + lista de contactos.
 * Se muestra cuando el permiso fue concedido.
 */
@Composable
private fun ContactsContent(
    uiState: ContactsUiState,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ─── Barra de búsqueda ────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Buscar contacto…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            singleLine = true
        )

        // ─── Contenido según estado ───────────────────────────────────────
        when (uiState) {

            is ContactsUiState.Idle -> { /* El LaunchedEffect iniciará la carga */ }

            is ContactsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando contactos…")
                    }
                }
            }

            is ContactsUiState.Success -> {
                if (uiState.contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank())
                                "No hay contactos en el teléfono."
                            else
                                "No se encontraron contactos para \"$searchQuery\".",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    /*
                     * LazyColumn: equivalente al RecyclerView.
                     * Solo renderiza los items VISIBLES en pantalla.
                     * Esencial para listas grandes (cientos de contactos).
                     *
                     * `key = { contact.id }`: ayuda a Compose a identificar
                     * qué items cambiaron, mejorando el rendimiento en
                     * inserciones, eliminaciones y reordenamientos.
                     */
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = uiState.contacts,
                            key = { contact -> contact.id }
                        ) { contact ->
                            ContactItem(contact = contact)
                        }
                    }
                }
            }

            is ContactsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pantalla de solicitud de permiso (primera vez).
 *
 * BUENA PRÁCTICA: Explica brevemente para qué se usará el permiso
 * ANTES de lanzar el diálogo del sistema. Mejora la tasa de aceptación.
 */
@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    PermissionInfoLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = "Acceso a Contactos",
        description = "Esta app necesita leer los contactos de tu teléfono para mostrarlos en pantalla.\n\nSe solicitará el permiso READ_CONTACTS.",
        button = {
            Button(onClick = onRequestPermission) {
                Text("Conceder permiso")
            }
        }
    )
}

/**
 * Pantalla de justificación (rationale).
 * Se muestra cuando el usuario ya denegó el permiso UNA vez.
 *
 * BUENA PRÁCTICA: Explica con más detalle POR QUÉ la app lo necesita,
 * sin ser invasivo. Nunca pedir el permiso en un bucle infinito.
 */
@Composable
private fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit
) {
    PermissionInfoLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = "Permiso Requerido",
        description = "Sin el permiso READ_CONTACTS no podemos mostrar los contactos.\n\n" +
                "Este permiso solo se usa para listar los datos dentro de la app. " +
                "No se comparte ni se envía ningún dato a servidores externos.",
        button = {
            Button(onClick = onRequestPermission) {
                Text("Entendido, continuar")
            }
        }
    )
}

/**
 * Pantalla para permiso permanentemente denegado.
 * Cuando el usuario marcó "No preguntar de nuevo", el sistema ya no
 * muestra el diálogo. La única opción es ir manualmente a Ajustes.
 */
@Composable
private fun PermissionPermanentlyDeniedScreen() {
    val context = LocalContext.current

    PermissionInfoLayout(
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = "Permiso Bloqueado",
        description = "El permiso fue bloqueado permanentemente.\n\n" +
                "Para usarlo, ve a:\n" +
                "Ajustes → Aplicaciones → Contactos → Permisos",
        button = {
            OutlinedButton(
                onClick = {
                    // Abre la pantalla de ajustes de la app en el sistema
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Abrir Ajustes")
            }
        }
    )
}

/**
 * Layout reutilizable para las pantallas de permiso.
 * Evita repetir el mismo código de diseño en las tres pantallas anteriores.
 */
@Composable
private fun PermissionInfoLayout(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    button: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            button()
        }
    }
}

