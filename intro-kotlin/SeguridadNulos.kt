fun ejemploNulos() {
    // Por defecto, no pueden ser null. En Java todo objeto puede ser null.
    var nombreUsuario: String = "Juan"
    
    // El '?' permite nulos
    var descripcion: String? = null 
    
    // Safe Call (?.): Si descripcion es null, no explota (NPE), retorna null.
    println(descripcion?.length) 
    
    // Operador Elvis (?:): Provee un valor por defecto si es nulo.
    val longitud = descripcion?.length ?: 0
}