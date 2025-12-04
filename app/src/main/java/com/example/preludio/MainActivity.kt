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
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
//                PreludioApp()
                // Información para el loggeo, por defecto se asume que es falso
                var isLoggedin by remember { mutableStateOf(false)}
                var currentUser by remember { mutableStateOf<Usuario?>(null)}

                if (isLoggedin && currentUser != null){
                    //Hay loggeo
                    PreludioApp(currentUser = currentUser!!) // Hace que User? sea User nomrla
                }else{
                    LoginApp(
                        onLoginSuccess = { user ->
                            currentUser = user
                            isLoggedin = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginApp(onLoginSuccess: (Usuario) -> Unit){ // Recursivo como en Pybodrio, se llama a sí mismo
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistrando by remember { mutableStateOf(false) }
    var context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // Autocompletado de android studio, pero
                                                        // Entiendo que es ccomo align items en html
    ) {
        Text(
            text = if (isRegistrando) "Registrarse" else "Iniciar sesión",
            fontSize = 24.sp,
                                // Otra vez autocompletado watafak
        )
        Spacer(modifier = Modifier.height(32.dp)) // Es como hr en html, un espaciado o salto
        OutlinedTextField(
            value = email,
            onValueChange = { email = it }, // Ya me da miedo el autoompletar
            label = {Text("Email")},
            leadingIcon = {Icon(Icons.Default.Email, contentDescription = null)}, // otro autocompletado, pero tiene sentido
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {Text("Contraseña")},
            visualTransformation = PasswordVisualTransformation(), //Esconde los caracteres
            leadingIcon = {Icon(Icons.Default.Lock, contentDescription = null)},
            modifier = Modifier.fillMaxWidth() // No confundir con size
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                // Correo válido
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Toast.makeText(context, "Email inválido", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Contraseña no corta
                if (password.length < 4){
                    Toast.makeText(context, "Contraseña inválida", Toast.LENGTH_SHORT).show()
                    return@Button

                }
                // Lógica si se registra un usuario
                if(isRegistrando){
                    val Nuevo = Usuario(email.split("@")[0], email, false)
                    // Crea un nuevo usuario y se asume que el inicio del correo es el nombre
                    onLoginSuccess(Nuevo) // Sale de la función proque ya no es null
                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()

                }
                // Ya sebería existir el admin
                else{
                    val isAdmin = email.endsWith("@admin.com")
                    val user = Usuario(email.split("@")[0], email, isAdmin)
                    onLoginSuccess(user)
                    Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                }
                // Se asume que el usuario igual existe, tengo que solucionarlo con la database
            }, modifier = Modifier.fillMaxWidth()
        ){
            Text(if (isRegistrando) "Registrarse" else "Iniciar sesión") // Colocar opción
        }
        TextButton(onClick = { isRegistrando = !isRegistrando }) {
            Text(if (isRegistrando) "Iniciar sesión" else "Registrarse")
        }
        // Es como los links url, un boton de texto, cambia al estado contrario porque solo hay 2 posibles casos
    }
}

//@PreviewScreenSizes
@Composable
fun PreludioApp(currentUser: Usuario) {
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