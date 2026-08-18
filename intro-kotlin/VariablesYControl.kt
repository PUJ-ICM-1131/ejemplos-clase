/**
 * DIFERENCIAS CON JAVA:
 * 1. No se usa ';' al final de las líneas.
 * 2. Los tipos se infieren automáticamente.
 */
fun main() {
    // val (Java: final) vs var (Variable normal)
    val tituloApp = "Gestor de Tareas" 
    var tareasPendientes = 5 // Inferencia de tipo Int
    
    // String Templates (Java: String.format() o concatenación con +)
    println("Bienvenido a $tituloApp. Tienes $tareasPendientes pendientes.")

    // 'when' como expresión (Java: switch no retorna valores directamente)
    val prioridad = 2
    val nivel = when (prioridad) {
        1 -> "Alta"
        2 -> "Media"
        in 3..5 -> "Baja" // Rangos con 'in'
        else -> "Sin prioridad"
    }
}