package com.icm2610.contactos.model

/**
 * MODELO DE DATOS: Contact
 * ══════════════════════════════════════════════════════════════════════
 *
 * Representa un contacto del teléfono.
 *
 * ¿Por qué `data class`?
 * ─────────────────────
 * Kotlin genera automáticamente:
 *  • equals() / hashCode()  → comparación por valor, no por referencia
 *  • toString()             → útil para depuración
 *  • copy()                 → crear copias modificadas fácilmente
 *
 * ¿Por qué `val` en todos los campos?
 * ─────────────────────────────────────
 * Inmutabilidad: un objeto inmutable no puede cambiar después de ser
 * creado. Esto evita bugs difíciles de rastrear y es una buena práctica
 * en programación funcional y en Compose (recomposición predecible).
 *
 * Si necesitamos "modificar" un contacto, usamos copy():
 *   val actualizado = contacto.copy(name = "Nuevo nombre")
 *
 * @param id          Identificador único del contacto en el sistema.
 * @param name        Nombre para mostrar.
 * @param phoneNumbers Lista de teléfonos (un contacto puede tener varios).
 * @param photoUri    URI de la foto; null si el contacto no tiene foto.
 */
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumbers: List<String>,
    val photoUri: String? = null  // '?' indica que puede ser null (nullable)
)

