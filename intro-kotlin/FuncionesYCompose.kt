/**
 * En Compose, los componentes son funciones @Composable.
 * Las funciones pueden recibir otras funciones como parámetros (Callbacks).
 */
fun main() {
    // Sintaxis de Trailing Lambda: Muy común en botones de Compose
    BotonPersonalizado(etiqueta = "Guardar") {
        // Este bloque es la función que se ejecuta al hacer click
        println("Acción ejecutada")
    }
}

// Parámetros con valores por defecto (Evita sobrecarga de métodos en Java)
fun BotonPersonalizado(etiqueta: String, onClick: () -> Unit = {}) {
    println("Dibujando botón: $etiqueta")
    onClick()
}