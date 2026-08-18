**
 * Data Class: Genera automáticamente toString, equals, copy.
 * En Java requiere muchas líneas de código (POJO).
 */
data class Producto(
    val id: Int,
    val nombre: String,
    val precio: Double
)

/**
 * Object (Singleton): Un solo objeto para toda la app.
 */
object InventarioFalso {
    val lista = listOf(
        Producto(1, "Laptop", 1200.0),
        Producto(2, "Mouse", 25.0)
    )
}

/**
 * Estado en Compose: Uso de remember y mutableStateOf.
 */
// Este es el "corazón" de la reactividad en Android nativo.
// val contador by remember { mutableStateOf(0) } // Sintaxis típica en Compose
