package com.icm.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.icm.camera.ui.CameraIntentScreen
import com.icm.camera.ui.CameraXScreen
import com.icm.camera.ui.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/*
 * Navigation 3 representa cada destino mediante una clave tipada. Estas claves reemplazan
 * las rutas de texto que usaba Navigation Compose 2, por lo que un error de escritura en una
 * ruta ya no puede enviar al usuario a un destino inexistente.
 *
 * @Serializable permite que rememberNavBackStack guarde y restaure la clave después de una
 * rotación de pantalla o de que Android destruya y recree el proceso de la aplicación.
 */
@Serializable
private data object MainRoute : NavKey

@Serializable
private data object CameraIntentRoute : NavKey

@Serializable
private data object CameraXRoute : NavKey

/**
 * Única Activity de la aplicación y punto de entrada declarado en AndroidManifest.xml.
 *
 * ComponentActivity ofrece integración con Jetpack Compose. La Activity no crea vistas XML:
 * instala directamente el árbol de componentes Compose mediante [setContent].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que la interfaz se dibuje detrás de las barras del sistema. Scaffold y los
        // componentes de Material calculan los espacios seguros necesarios para el contenido.
        enableEdgeToEdge()

        // setContent inicia la interfaz declarativa. Cuando cambia un estado observado dentro
        // de este bloque, Compose vuelve a ejecutar solamente las partes afectadas.
        setContent {
            // MaterialTheme proporciona colores, tipografías y formas de Material 3 a todos
            // los componentes descendientes.
            MaterialTheme {
                // Surface crea el fondo principal y ocupa todo el espacio disponible.
                Surface(modifier = Modifier.fillMaxSize()) {
                    CameraApp()
                }
            }
        }
    }
}

/**
 * Componente raíz de la aplicación.
 *
 * Aquí se coordinan tres responsabilidades globales: el historial de Navigation 3, el estado
 * del menú lateral y la estructura visual formada por el drawer, la barra superior y el área
 * donde se muestra la pantalla activa.
 */
@Composable
fun CameraApp() {
    // Navigation 3 deja que la aplicación sea propietaria del historial. La última clave de
    // esta lista es el destino visible. MainRoute es la pantalla inicial.
    val backStack = rememberNavBackStack(MainRoute)

    // DrawerState conserva si el menú lateral está abierto o cerrado entre recomposiciones.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Abrir y cerrar el drawer son operaciones suspendidas y animadas. Este scope permite
    // ejecutarlas sin bloquear el hilo principal y se cancela al salir de la composición.
    val coroutineScope = rememberCoroutineScope()

    // ModalNavigationDrawer coloca el panel lateral encima del contenido cuando se abre.
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                backStack = backStack,
                coroutineScope = coroutineScope,
                drawerState = drawerState
            )
        }
    ) {
        // Scaffold organiza la barra superior y entrega innerPadding con el espacio que esta
        // ocupa. Aplicar ese padding evita que las pantallas queden ocultas bajo la barra.
        Scaffold(
            topBar = {
                CameraTopAppBar(
                    drawerState = drawerState,
                    coroutineScope = coroutineScope
                )
            }
        ) { innerPadding ->
            NavigationContent(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Barra superior compartida por todas las pantallas.
 *
 * El botón de navegación abre el drawer mediante una corrutina porque [DrawerState.open] es
 * una función suspendida que ejecuta la animación de apertura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTopAppBar(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope
) {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        title = { Text(text = "Camera App") },
        navigationIcon = {
            IconButton(
                onClick = {
                    coroutineScope.launch { drawerState.open() }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Abrir menú de navegación"
                )
            }
        }
    )
}

/**
 * Contenido del menú lateral.
 *
 * [backStack] se consulta para resaltar el destino visible y también se modifica cuando el
 * usuario selecciona una opción. Cada opción se trata como un destino principal: se conserva
 * [MainRoute] en la base y se reemplaza cualquier pantalla que estuviera encima.
 */
@Composable
private fun DrawerContent(
    backStack: NavBackStack<NavKey>,
    coroutineScope: CoroutineScope,
    drawerState: DrawerState
) {
    // Navigation 3 muestra la última clave del back stack; por eso esta comparación permite
    // saber qué elemento del drawer debe aparecer seleccionado.
    val currentRoute = backStack.lastOrNull()

    /**
     * Cambia a uno de los destinos principales sin acumular copias de la misma pantalla.
     * Después inicia la animación que cierra el menú.
     */
    fun navigateTo(route: NavKey) {
        if (currentRoute != route) {
            backStack.clear()
            backStack.add(MainRoute)
            if (route != MainRoute) {
                backStack.add(route)
            }
        }
        coroutineScope.launch { drawerState.close() }
    }

    ModalDrawerSheet {
        Text(
            text = "Funcionalidades de cámara",
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text(text = "Cámara Intent") },
            selected = currentRoute == CameraIntentRoute,
            onClick = { navigateTo(CameraIntentRoute) }
        )

        NavigationDrawerItem(
            label = { Text(text = "CameraX") },
            selected = currentRoute == CameraXRoute,
            onClick = { navigateTo(CameraXRoute) }
        )
    }
}

/**
 * Convierte las claves almacenadas en [backStack] en pantallas Compose.
 *
 * [NavDisplay] observa la lista y representa la última entrada. El entryProvider define de
 * forma tipada qué composable corresponde a cada clase de ruta. Al usar el gesto o botón de
 * retroceso se elimina la última clave, siempre que quede [MainRoute] como pantalla raíz.
 */
@Composable
private fun NavigationContent(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<MainRoute> {
                MainScreen()
            }
            entry<CameraIntentRoute> {
                CameraIntentScreen()
            }
            entry<CameraXRoute> {
                CameraXScreen()
            }
        }
    )
}

/**
 * Vista previa utilizada por Android Studio para renderizar la aplicación sin instalarla.
 */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CameraApp()
}
