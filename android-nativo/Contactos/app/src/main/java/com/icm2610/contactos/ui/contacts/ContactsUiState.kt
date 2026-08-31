package com.icm2610.contactos.ui.contacts

import com.icm2610.contactos.model.Contact

/**
 * ESTADO DE LA UI: ContactsUiState
 * ══════════════════════════════════════════════════════════════════════
 *
 * PATRÓN UiState con `sealed class`
 * ──────────────────────────────────
 * Modela todos los estados posibles de la pantalla de contactos.
 *
 * ¿Por qué `sealed class` y no un enum o una clase normal?
 *  • `sealed class`: los subtipos pueden tener datos distintos entre sí.
 *  • `enum`: todos los valores tienen la misma estructura.
 *  • Ventaja clave: el compilador verifica que el `when` sea exhaustivo,
 *    es decir, que cubramos TODOS los estados posibles.
 *
 * Flujo típico de estados:
 *
 *   Idle ──→ Loading ──→ Success(contacts)
 *                   └──→ Error(message)
 *
 * El ViewModel emite estos estados a través de un StateFlow.
 * La UI observa el StateFlow y se redibuja según el estado actual.
 */
sealed class ContactsUiState {

    /** Estado inicial. La pantalla acaba de abrirse y aún no hay acción. */
    data object Idle : ContactsUiState()

    /** Cargando los contactos desde el ContentProvider. */
    data object Loading : ContactsUiState()

    /**
     * Contactos cargados exitosamente.
     * @param contacts Lista de contactos para mostrar (puede estar vacía).
     */
    data class Success(val contacts: List<Contact>) : ContactsUiState()

    /**
     * Ocurrió un error al cargar los contactos.
     * @param message Descripción del error para mostrar al usuario.
     */
    data class Error(val message: String) : ContactsUiState()
}

