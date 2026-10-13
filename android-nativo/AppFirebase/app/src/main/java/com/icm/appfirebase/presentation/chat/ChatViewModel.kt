package com.icm.appfirebase.presentation.chat // Asegúrate que el paquete sea correcto

import android.app.Application // Necesario para el contexto
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Cambia ViewModel a AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.Firebase
import com.google.firebase.firestore.snapshots // Asegúrate que este import esté
import com.icm.appfirebase.data.model.Message
import com.icm.appfirebase.data.model.User // Importa tu modelo User
import com.icm.appfirebase.core.NotificationUtils // Importa tu utilidad de notificación
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async // Importa async para búsquedas paralelas

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Flow para el texto actual en el campo de entrada
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // Flow para la lista de mensajes (se actualiza en tiempo real)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Flow para errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- Estado para guardar detalles de usuarios ---
    private val _userDetails = MutableStateFlow<Map<String, User>>(emptyMap())
    val userDetails: StateFlow<Map<String, User>> = _userDetails.asStateFlow()
    // Guarda los IDs de los que ya intentamos buscar para no repetir
    private val fetchedUserIds = mutableSetOf<String>()
    // --- Fin Estado userDetails ---

    // --- Estado para saber si la pantalla de chat está activa ---
    private val _isChatScreenActive = MutableStateFlow(false)
    // val isChatScreenActive: StateFlow<Boolean> = _isChatScreenActive.asStateFlow() // No es necesario exponerlo si solo lo usa el VM

    fun setChatScreenActive(isActive: Boolean) {
        _isChatScreenActive.value = isActive
        if(isActive) {
            Log.d("ChatViewModel", "Chat screen is NOW ACTIVE")
        } else {
            Log.d("ChatViewModel", "Chat screen is NOW INACTIVE")
        }
    }
    // --- Fin Estado isChatScreenActive ---

    private var previousMessageIds: Set<String>? = null

    init {
        listenForMessages()
    }

    // Actualiza el texto del mensaje
    fun onMessageTextChange(newText: String) {
        _messageText.value = newText
    }

    // Escucha cambios en la colección 'messages' en tiempo real
    private fun listenForMessages() {
        db.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(100)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Message>() }
            .onEach { currentMessageList ->
                // Lógica para detectar nuevos mensajes y notificar
                val knownIds = previousMessageIds
                if (knownIds != null) {
                    // La consulta conserva solo 100 mensajes: el tamaño puede no cambiar.
                    val newMessages = currentMessageList.filter { it.id !in knownIds }
                    newMessages.forEach { newMessage ->
                        if (newMessage.senderId != auth.currentUser?.uid && !_isChatScreenActive.value) {
                            val senderDetails = _userDetails.value[newMessage.senderId]
                            val senderName = senderDetails?.name?.takeIf { it.isNotBlank() }
                                ?: senderDetails?.email
                                ?: "Someone"

                            Log.d("ChatViewModel", "New message from $senderName, chat inactive. Showing notification.")
                            NotificationUtils.showNewMessageNotification(
                                getApplication<Application>().applicationContext,
                                senderName,
                                newMessage.text
                            )

                        }
                    }
                }
                previousMessageIds = currentMessageList.map { it.id }.toSet()

                _messages.value = currentMessageList
                fetchUserDetailsIfNeeded(currentMessageList) // Llama a buscar detalles
            }
            .catch { exception ->
                Log.e("ChatViewModel", "Error listening for messages", exception)
                _error.value = "Failed to load messages: ${exception.message}"
            }
            .launchIn(viewModelScope)
    }

    // Función para buscar detalles de usuarios
    private fun fetchUserDetailsIfNeeded(messages: List<Message>) {
        val currentUserId = auth.currentUser?.uid
        val senderIdsToFetch = messages
            .map { it.senderId }
            .distinct()
            .filter { it.isNotBlank() && it != currentUserId && !fetchedUserIds.contains(it) }
            .toSet()

        if (senderIdsToFetch.isNotEmpty()) {
            Log.d("ChatViewModel", "Fetching details for IDs: $senderIdsToFetch")
            fetchedUserIds.addAll(senderIdsToFetch)

            viewModelScope.launch {
                senderIdsToFetch.forEach { userId ->
                    async { // Ejecuta búsquedas en paralelo
                        try {
                            val userDoc = db.collection("users").document(userId).get().await()
                            val user = userDoc.toObject(User::class.java)
                            if (user != null) {
                                _userDetails.value = _userDetails.value + (userId to user)
                                Log.d("ChatViewModel", "Details fetched for $userId: ${user.name}")
                            } else {
                                Log.w("ChatViewModel", "User document not found for ID: $userId")
                            }
                        } catch (e: Exception) {
                            Log.e("ChatViewModel", "Error fetching user details for $userId", e)
                            fetchedUserIds.remove(userId) // Permite reintento
                        }
                    }
                }
            }
        }
    }

    // Envía un nuevo mensaje a Firestore
    fun sendMessage() {
        val textToSend = _messageText.value.trim()
        val currentUserId = auth.currentUser?.uid

        if (textToSend.isBlank()) return
        if (currentUserId == null) return

        _messageText.value = ""
        // _error.value = null // El error se maneja en la UI (Snackbar)

        val newMessage = Message(
            text = textToSend,
            senderId = currentUserId
        )

        viewModelScope.launch {
            try {
                db.collection("messages").add(newMessage).await()
                Log.d("ChatViewModel", "Message sent successfully!")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}