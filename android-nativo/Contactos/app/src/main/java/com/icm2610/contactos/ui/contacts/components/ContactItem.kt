package com.icm2610.contactos.ui.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icm2610.contactos.model.Contact
import kotlin.math.absoluteValue

/**
 * COMPONENTE: ContactItem
 * ══════════════════════════════════════════════════════════════════════
 *
 * Composable que representa UNA fila de la lista de contactos.
 *
 * BUENAS PRÁCTICAS EN COMPOSE:
 * ─────────────────────────────
 *  • Componentes pequeños y reutilizables (Single Responsibility).
 *  • Recibe solo lo que necesita (Contact) y no el ViewModel completo.
 *  • Usa `remember` para calcular el color del avatar una sola vez
 *    por instancia, evitando recálculos en cada recomposición.
 *  • Incluye @Preview para poder visualizarlo en el IDE sin emulador.
 *
 * @param contact  El contacto a mostrar.
 * @param modifier Modifier externo para personalización (buena práctica
 *                 exponer el Modifier en todos los composables públicos).
 */
@Composable
fun ContactItem(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ─── Avatar con iniciales ─────────────────────────────────────
            ContactAvatar(name = contact.name)

            Spacer(modifier = Modifier.width(12.dp))

            // ─── Nombre y teléfonos ───────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                contact.phoneNumbers.forEach { number ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Teléfono",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Avatar circular con la(s) inicial(es) del nombre del contacto.
 *
 * El color se asigna deterministamente usando el hash del nombre,
 * de modo que cada contacto siempre tenga el mismo color.
 */
@Composable
private fun ContactAvatar(name: String) {
    // `remember(name)` → recalcula solo cuando cambia el nombre
    val avatarColor = remember(name) {
        val palette = listOf(
            Color(0xFF1565C0), // Azul
            Color(0xFFAD1457), // Rosa
            Color(0xFF2E7D32), // Verde
            Color(0xFFE65100), // Naranja
            Color(0xFF6A1B9A), // Morado
            Color(0xFF00695C), // Teal
            Color(0xFF4527A0), // Indigo
            Color(0xFF558B2F)  // Verde oliva
        )
        palette[name.hashCode().absoluteValue % palette.size]
    }

    val initials = remember(name) {
        // Toma las primeras letras de las primeras dos palabras
        name.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun ContactItemPreview() {
    ContactItem(
        contact = Contact(
            id = 1L,
            name = "María García",
            phoneNumbers = listOf("+54 11 1234-5678", "+54 11 8765-4321")
        )
    )
}

