package com.icm2610.contactos.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.icm2610.contactos.data.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * VIEWMODEL: ContactsViewModel
 * ══════════════════════════════════════════════════════════════════════
 *
 * PATRÓN MVVM (Model-View-ViewModel)
 * ────────────────────────────────────
 *   Model      → Contact (datos) + ContactsRepository (acceso a datos)
 *   View       → ContactsScreen (UI en Compose)
 *   ViewModel  → ContactsViewModel (lógica de presentación)
 *
 * Responsabilidades del ViewModel:
 *  1. Sobrevivir cambios de configuración (rotar pantalla, cambiar idioma).
 *     Sin ViewModel, onCreate() se vuelve a llamar y se pierden los datos.
 *  2. Exponer el estado de la UI mediante StateFlow (observable).
 *  3. Invocar el Repository para obtener/manipular datos.
 *  4. NO tener referencias a Activity, Fragment ni Composable.
 *     (evita memory leaks y acoplamientos innecesarios)
 *
 * AndroidViewModel vs ViewModel:
 * ────────────────────────────────
 *  • ViewModel         → no conoce el contexto de Android.
 *  • AndroidViewModel  → recibe el Application, necesario para acceder
 *                        al ContentResolver sin retener la Activity.
 *
 * NOTA ARQUITECTURA AVANZADA:
 * En proyectos reales se usaría Hilt (inyección de dependencias) para
 * proveer el Repository al ViewModel, evitando la instanciación manual.
 * Ejemplo con Hilt: @HiltViewModel + @Inject constructor(repo: ContactsRepository)
 */
class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    // ─── Repositorio ─────────────────────────────────────────────────────────
    // En una arquitectura con Hilt, esto sería: @Inject constructor(private val repository: ContactsRepository)
    private val repository = ContactsRepository(application.contentResolver)

    // ─── Estado de la UI (StateFlow) ─────────────────────────────────────────
    /*
     * MutableStateFlow: flujo de datos que mantiene siempre un valor actual.
     *
     * Patrón de encapsulamiento:
     *  _uiState  → privado, mutable  (solo el ViewModel puede cambiar el estado)
     *   uiState  → público, inmutable (la UI solo puede leer, nunca escribir)
     *
     * Esto garantiza que el flujo de datos sea UNI-DIRECCIONAL:
     *   ViewModel ──emite estado──→ UI ──dispara eventos──→ ViewModel
     */
    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Idle)
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    // ─── Búsqueda ─────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Cache de todos los contactos sin filtrar
    private var allContacts: List<com.icm2610.contactos.model.Contact> = emptyList()

    // ─── Funciones públicas (eventos desde la UI) ─────────────────────────────

    /**
     * Carga los contactos del teléfono.
     * Llamado desde la UI cuando el permiso ha sido concedido.
     *
     * `viewModelScope.launch`:
     *  • Lanza una coroutine en el scope del ViewModel.
     *  • Se cancela automáticamente cuando el ViewModel es destruido,
     *    evitando trabajo innecesario y crashes por referencias inválidas.
     */
    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = ContactsUiState.Loading
            try {
                allContacts = repository.getContacts()
                applyFilter()                      // Aplica búsqueda actual (si hay)
            } catch (e: SecurityException) {
                // SecurityException: el permiso fue revocado mientras cargaba
                _uiState.value = ContactsUiState.Error(
                    "Permiso de contactos revocado. Por favor, reinicia la app."
                )
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.Error(
                    "Error al cargar los contactos: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza el texto de búsqueda y filtra la lista en tiempo real.
     * Llamado desde el TextField de búsqueda en la UI.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    // ─── Funciones privadas (lógica interna) ──────────────────────────────────

    /**
     * Filtra la lista de contactos según la consulta de búsqueda actual.
     * Busca tanto en el nombre como en los números de teléfono.
     */
    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val filtered = if (query.isBlank()) {
            allContacts
        } else {
            allContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumbers.any { number -> number.contains(query) }
            }
        }
        _uiState.value = ContactsUiState.Success(filtered)
    }
}

