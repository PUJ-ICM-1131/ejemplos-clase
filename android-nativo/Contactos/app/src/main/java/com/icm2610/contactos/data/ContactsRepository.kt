package com.icm2610.contactos.data

import android.content.ContentResolver
import android.provider.ContactsContract
import com.icm2610.contactos.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * REPOSITORIO: ContactsRepository
 * ══════════════════════════════════════════════════════════════════════
 *
 * PATRÓN REPOSITORY
 * ─────────────────
 * El Repository es la "única fuente de verdad" de los datos.
 * Abstrae la fuente de datos del resto de la aplicación.
 *
 *   ViewModel  ──→  Repository  ──→  ContentProvider (contactos)
 *
 * Ventajas:
 *  • El ViewModel no sabe si los datos vienen de la red, BD local o SO.
 *  • Si mañana guardamos contactos en Room DB, solo cambia el Repository.
 *  • Facilita las pruebas (se puede usar un FakeRepository en tests).
 *
 * CONTENT PROVIDER
 * ────────────────
 * Android expone los contactos a través de un ContentProvider, que es
 * una base de datos SQLite compartida entre aplicaciones.
 * Se accede mediante un ContentResolver con URIs del tipo:
 *
 *   content://com.android.contacts/data/phones
 *
 * @param contentResolver  Provisto por el contexto de la aplicación.
 */
class ContactsRepository(
    private val contentResolver: ContentResolver
) {

    /**
     * Obtiene todos los contactos del teléfono.
     *
     * `suspend fun` + `withContext(Dispatchers.IO)`:
     * ──────────────────────────────────────────────
     * Las operaciones de I/O (disco, red, BD) NUNCA deben ejecutarse
     * en el hilo principal (Main/UI thread), porque bloquearían la UI
     * y produciría el temido "Application Not Responding" (ANR).
     *
     * withContext(Dispatchers.IO) cambia la ejecución a un pool de
     * hilos optimizado para operaciones de entrada/salida.
     *
     * @return Lista de contactos ordenada alfabéticamente.
     */
    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {

        // MutableMap para agrupar múltiples números del mismo contacto
        val contactsMap = mutableMapOf<Long, Contact>()

        /*
         * PROYECCIÓN (qué columnas queremos leer)
         * Es como el "SELECT col1, col2 FROM tabla" en SQL.
         * Leer solo las columnas necesarias mejora el rendimiento.
         */
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        /*
         * QUERY al ContentProvider de Contactos
         * Parámetros del query:
         *   uri        → qué tabla consultar (teléfonos de contactos)
         *   projection → qué columnas leer
         *   selection  → filtro WHERE (null = todos)
         *   selArgs    → argumentos del filtro
         *   sortOrder  → ORDER BY nombre ascendente
         */
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,  // URI de la tabla
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        /*
         * `cursor?.use { }`:
         *  • '?' — operador de llamada segura: solo ejecuta si cursor != null
         *  • 'use' — cierra el cursor automáticamente al terminar el bloque,
         *    incluso si ocurre una excepción (evita memory leaks)
         */
        cursor?.use { c ->

            // Obtenemos los índices de cada columna UNA sola vez (fuera del loop)
            // Llamar getColumnIndex() dentro del while sería ineficiente
            val idIndex     = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex   = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex  = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            // Iteramos fila por fila (como un iterador de BD)
            while (c.moveToNext()) {
                val id     = c.getLong(idIndex)
                val name   = c.getString(nameIndex)   ?: "Sin nombre"
                val number = c.getString(numberIndex) ?: ""
                val photo  = c.getString(photoIndex)  // puede ser null

                /*
                 * Un contacto puede tener múltiples números de teléfono.
                 * El ContentProvider devuelve UNA fila por número.
                 * Los agrupamos por CONTACT_ID usando el Map.
                 */
                val existing = contactsMap[id]
                if (existing != null) {
                    // El contacto ya existe: agregamos el número a su lista
                    contactsMap[id] = existing.copy(
                        phoneNumbers = existing.phoneNumbers + number
                    )
                } else {
                    // Primer número de este contacto: creamos la entrada
                    contactsMap[id] = Contact(
                        id           = id,
                        name         = name,
                        phoneNumbers = listOf(number),
                        photoUri     = photo
                    )
                }
            }
        }

        // Devolvemos la lista de contactos (los valores del Map)
        contactsMap.values.toList()
    }
}

