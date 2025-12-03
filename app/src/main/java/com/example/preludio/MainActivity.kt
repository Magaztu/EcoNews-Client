package com.example.preludio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.preludio.ui.theme.PreludioTheme

data class Usuario(
    val nombre: String,
    val email: String,
    val isAdmin: Boolean,
    var bio: String = "",
    var carrera: String  =""
)
// Por ahora usaré usuarios como objetos, no sé si sea compatible con la db

data class Post(
    val id: Int,
    val autor: String,
    val contenido: String,
    val categoria: String = "General",
    val fecha: String = "Ahora", // Debería cambiarlo por un date.now().toString o algo asi
)

data class Evento(
    val id: Int,
    val titulo: String,
    val fecha: String,
    val descripcion: String,
    var isRegistrado: Boolean = false
)

data class Servicio(
    val nombre: String,
    val icon: ImageVector, // supongo que es como los enums
    val info: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Crea pantalla completa
        enableEdgeToEdge()
        setContent {
            PreludioTheme {
                PreludioApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun PreludioApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ANUNCIOS) }
    // Guarda la posición actual del usuario en la app, similar a los checkboxes

    //Esta parte en verde se encarga de definir la posición de la barra de navegación
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            // Bucle para cada enum, crea los botones de la barra
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        // El modifier que hemos visto en clase está aquí
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Greeting(
                name = "Android",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// Este ENUM vino con la plantilla y define los botones de la barrita
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    ANUNCIOS("Anuncios", Icons.Default.Notifications),
    COMUNIDAD("Comunidad", Icons.Default.ShoppingCart),
    SERVICIOS("Servicios", Icons.Default.Search),
    EVENTOS("Eventos", Icons.Default.DateRange),
    PERFIL("Perfil", Icons.Default.AccountCircle),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // Muestra un bloque de texto en pantalla
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// Preview para greeting que permtie ver su uso sin compilar el proyecto
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    PreludioTheme {
//        Greeting("Android")
//    }
//}