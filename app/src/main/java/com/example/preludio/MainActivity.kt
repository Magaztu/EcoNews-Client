package com.example.preludio

// Configuración de la conexión al backend
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
// Métodos HTTP
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
// Usar websockets
import io.socket.client.IO
import io.socket.client.Socket
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.google.gson.annotations.SerializedName

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

//data class Post(
//    val id: String, // porque la databsae usa string para whatsapapi
//    val autor: String,
//    val contenido: String,
//    val categoria: String = "General",
//    val fecha: String = "Ahora", // Debería cambiarlo por un date.now().toString o algo asi
//)

data class Post(
    // Serialized indica cuál cabecera del body JSON corresponde a cada campo
    @SerializedName("whatsappId")
    val id: String = "",
    @SerializedName("from")
    val autor: String = "Sistema",
    @SerializedName("body")
    val contenido: String,
    val categoria: String = "General",
    @SerializedName("createdAt")
    val fecha: String = ""
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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

    var showCreateDialog by remember { mutableStateOf(false) }
    // Para mostrar dialogos en algunas partes

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
        Scaffold(topBar = {
            if (currentDestination != AppDestinations.PERFIL){
                CenterAlignedTopAppBar(
                    title = {Text("EcoNews")},
                    actions = {
                        // Aqui pongo las bolitas de crear cosas si es user o admin
                        val canPostAnuncio = currentDestination == AppDestinations.ANUNCIOS && currentUser.isAdmin
                        val canPostComunidad = currentDestination == AppDestinations.COMUNIDAD
                        val canPostEvento = currentDestination == AppDestinations.EVENTOS && currentUser.isAdmin

                        if(canPostAnuncio || canPostComunidad || canPostEvento){
                            IconButton(onClick = {showCreateDialog = true}) {
                                Icon(Icons.Default.Add, contentDescription = "Crear")
                            }
                        }

                    }
                )
        }
        }) { innerPadding ->
            // Hace que la barrita de arriba tenga padding apra cada pantalla
            Box(modifier = Modifier.padding(innerPadding)){
                when (currentDestination){
                    AppDestinations.ANUNCIOS -> AnunciosScreen(currentUser)
                    AppDestinations.COMUNIDAD -> ComunidadScreen(currentUser)
                    AppDestinations.SERVICIOS -> ServiciosScreen(currentUser)
                    AppDestinations.EVENTOS -> EventosScreen(currentUser)
                    AppDestinations.PERFIL -> PerfilScreen(currentUser)
                }
            }
            if (showCreateDialog) {
                CreatePostDialog(
                    destination = currentDestination,
                    onDismiss = { showCreateDialog = false },
                    onSubmit = { text, categoria ->
                        if (currentDestination == AppDestinations.ANUNCIOS) {
                             scope.launch {
                                NetworkUtils.api.publishPost(PostBody(text))
                             }
                            Toast.makeText(context, "Enviando a WhatsApp...", Toast.LENGTH_SHORT).show()
                        }

                        showCreateDialog = false
                    }
                )
            }
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
fun AnunciosScreen(user: Usuario){
    // Simular entrdas hasta que me salga bien la base de datos
//    val posts = remember { listOf(
//        Post("1","Mathias", "Clases canceladas por el presidente", "General"),
//        Post("2","Dylan", "Mañana habre el nuevo complejo", "General"),
//        ) }
    val posts = remember { mutableStateListOf<Post>() }
    val scope = rememberCoroutineScope() // Permite lanzar las llamadas al API en segundo plano
    val context = LocalContext.current

    // Rutina de fetchear posts, para saber qué hay en el historial
    LaunchedEffect(Unit) {
        try {
            val history = NetworkUtils.api.getPosts()
            posts.clear() //limpiar lista
            posts.addAll(history) //añadir valores a la lista
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Configuración del websocket
    DisposableEffect(Unit) {
        var socket: Socket? = null
        try {
            socket = IO.socket(NetworkUtils.BASE_URL) // Se crea el socket hacia la apiii
            socket.connect()

            // Actuar cuando llega el evento "new_post"
            socket.on("new_post") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val gson = Gson()
                    // Mappear el JSON a la clase
                    try {
                        val newPost = gson.fromJson(data.toString(), Post::class.java)

                        scope.launch {
                            // Evitar duplicados o condiciones de carrera
                            if (posts.none { it.id == newPost.id }) {
                                posts.add(0, newPost)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Otro evento, post borrao
            socket.on("post_deleted") { args ->
                val data = args[0] as JSONObject
                val idToDelete = data.getString("id")
                scope.launch {
                    posts.removeAll { it.id == idToDelete }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        //Desconectarse al reensamblar la app
        onDispose {
            socket?.disconnect()
        }
    }

    // Los lazy columns son básicamente listas scrolleables
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)){
        item {Text("Anuncios oficiales", style = MaterialTheme.typography.titleMedium)}

        // Crea un "titulo" y luego para cada post muestra su info
        items(posts, key = {it.id}) {
            post -> Card(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(post.contenido, style = MaterialTheme.typography.bodyLarge)
                    Text(post.fecha, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (user.isAdmin) {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    NetworkUtils.api.deletePost(post.id)
                                    Toast.makeText(context, "Eliminando...", Toast.LENGTH_SHORT).show()
                                } catch(e: Exception) {
                                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("ELIMINAR", color = Color.Red)
                        }
                    }
                    // Borar sólo admins
                }
            }
        }
    }
}

@Composable
fun ComunidadScreen(user: Usuario){
    var filtro by remember { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Comida", "Servicios", "Materiales")
    val posts = listOf(
        Post("1", "Jorge", "Vendo calculadora", "Materiales"),
        Post("2","Maria","Se realizan perforaciones", "Servicios"),
        Post("3", "Paco", "Vendo chimichangas", "Comida")
    )
    val postsFiltrados = if (filtro == "Todos") posts else posts.filter { it.categoria == filtro }
    // Filtrar por categoria
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categorias.forEach { card ->
                // Estas son tarjetitas que filtran automaticamente el contenido
                // Sus opciones se parecen mucho a los checkboxes / radio buttons
                FilterChip(
                    selected = filtro == card,
                    onClick = { filtro = card },
                    label = { Text(card) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn{
            items(postsFiltrados){ post ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                            Text(post.autor, style = MaterialTheme.typography.titleSmall)
                            SuggestionChip(onClick = {}, label = { Text(post.categoria) })
                        }
                        Text(post.contenido)
                        // Borrar sólo si es admin o autor
                        if (user.isAdmin||user.nombre == post.autor){
                            TextButton(onClick = { }) { Text("ELIMINAR", color = Color.Red)}
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ServiciosScreen(user: Usuario){
    val servicios = listOf(
        Servicio("Ecobuses", Icons.Default.DirectionsBus, "Ruta 6: La Joya"),
        Servicio("Gimnasio", Icons.Default.FitnessCenter, "Abierto de 7:30 a 21:30, traer ropa deportiva"),
        Servicio("Naturissímo", Icons.Default.Restaurant,"Combos desde $3.99"),
        Servicio("Biblioteca", Icons.Default.LocalLibrary, "Registrarse con QR al ingresar")
    )
    var servicioElegido by remember { mutableStateOf<Servicio?>(null) }
    // Recordar el objeto seleccionado

    // Como lazycolumn, este también es escrolleable pero permite crear grids omgg
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(16.dp)) {
        items(servicios){
            servicio ->
                Card(
                    //Dar click a la tarjeta selecciona un servicio
                    modifier = Modifier.padding(8.dp).height(120.dp).clickable{servicioElegido = servicio},
                    // Este tipo de color viene de un repo, asi como los CSS frameworks
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(servicio.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                        Text(servicio.nombre, style = MaterialTheme.typography.titleMedium)
                        // Asignar icono y nombre del servicio a la cartajeta
                    }
                }
        }
    }
    if (servicioElegido != null)
        // Muestra en detalle la información del servicio cuando es elegido
        AlertDialog(
            onDismissRequest = {servicioElegido = null},
            icon = {Icon(servicioElegido!!.icon,"")},
            title = {Text(servicioElegido!!.nombre)},
            text = {Text(servicioElegido!!.info)},
            confirmButton = { TextButton(onClick = {servicioElegido = null}) { Text("Cerrar") }}
        )
}

@Composable
fun EventosScreen(user: Usuario){
    val eventos = remember { mutableStateListOf(
        Evento(1,"Casa Abierta", "Jueves 24", "Descubre clubes y amistades"),
        Evento(2,"Miércoles Cultural","Miércoles 23","Disfruta de música en vivo")
    ) }
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(eventos){ evento ->
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)){
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(evento.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(evento.fecha, color = Color.Blue)
                    Text(evento.descripcion)
                    Spacer(modifier = Modifier.height(8.dp))

                    if(evento.isRegistrado){
                        Button(
                            onClick = {evento.isRegistrado = false},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) { Text("Cancelar Registro") }
                    }
                    else {
                        Button(onClick = {evento.isRegistrado = true})
                        { Text("Registrarse") }
                    }
                    if (user.isAdmin){
                        TextButton(onClick = { }) { Text("ELIMINAR", color = Color.Red)}
                    }
                }
            }
        }
    }
}

@Composable
fun PerfilScreen(user: Usuario){
    var bio by remember { mutableStateOf(user.bio) }
    var carrera by remember { mutableStateOf(user.carrera) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Simula fotito de perfil
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Text(user.nombre, style = MaterialTheme.typography.headlineMedium)
        Text(user.email, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = carrera,
            onValueChange = { carrera = it },
            label = { Text("Career") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / Status") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Actualizar campos
            user.carrera = carrera
            user.bio = bio
            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
        }) {
            Text("Guardar cambios")
        }
    }
}

@Composable
fun CreatePostDialog(destination: AppDestinations, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit){
    var text by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Materiales") }
    val categorias = listOf("Materiales", "Servicios", "Comida")

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Crear post en ${destination.label}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Dialogos para crear psots en eventosm comunidad y anuncios
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)

                )
                //En el caso de comunidad, se puede elegir una categoria
                if(destination == AppDestinations.COMUNIDAD){
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Categoría:")

                    categorias.forEach { card ->
                        Row {
                            FilterChip(
                                selected = categoriaSeleccionada == card,
                                onClick = { categoriaSeleccionada = card },
                                label = { Text(card) },
                                modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar")}
                    Button(onClick = { onSubmit(text, categoriaSeleccionada) }) { Text("Post") }
                }
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    // Muestra un bloque de texto en pantalla
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

// Preview para greeting que permtie ver su uso sin compilar el proyecto
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    PreludioTheme {
//        Greeting("Android")
//    }
//}


// Clase que define a un objeto para enviar posts (lo pide la doc??)
data class PostBody(val text: String)

// Definir las rutas, similar a cuando usabamos Java.io.File
interface WahaApi {
    @GET("api/waha/posts")
    suspend fun getPosts(): List<Post>

    @POST("api/waha/publish")
    suspend fun publishPost(@Body body: PostBody): retrofit2.Response<Any>

    @DELETE("api/waha/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): retrofit2.Response<Any>
    // Aqui path es equivalente a params
}

// SINGLETON como en software 2 waos, object significa eso
object NetworkUtils {
    // Puerto y url para el emulador del cel
    const val BASE_URL = "http://10.0.2.2:3001/"

    val api: WahaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WahaApi::class.java)
    }
}