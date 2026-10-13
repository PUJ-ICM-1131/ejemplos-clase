package com.icm.appfirebase.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date // Usamos Date con @ServerTimestamp

data class Message(
    @DocumentId val id: String = "", // ID del documento, también para claves estables en Compose
    val text: String = "",
    val senderId: String = "", // UID del usuario que envía
    @ServerTimestamp // Firestore asignará la hora del servidor automáticamente
    val timestamp: Date? = null // Firestore convertirá esto a Timestamp internamente
) {
    // Constructor sin argumentos requerido por Firestore para toObjects()
    constructor() : this("", "", "", null)
}