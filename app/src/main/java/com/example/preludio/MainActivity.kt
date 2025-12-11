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
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.preludio.ui.theme.PreludioTheme

data class Usuario(
    val nombre: String,
    val email: String,
    val contrasena: String,
    val isAdmin: Boolean,
    var bio: String = "",
    var carrera: String = ""
)

data class Post(
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
    val icon: ImageVector,
    val info: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreludioTheme {
                var isLoggedin by remember { mutableStateOf(false) }
                var currentUser by remember { mutableStateOf<Usuario?>(null) }
                val users = remember { mutableStateListOf(Usuario("admin", "admin@admin.com", "admin", true)) }
                val context = LocalContext.current

                if (isLoggedin && currentUser != null) {
                    PreludioApp(
                        currentUser = currentUser!!,
                        onLogout = {
                            isLoggedin = false
                            currentUser = null
                            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    LoginApp(
                        onLoginRequest = { email, password ->
                            val user = users.find { it.email == email && it.contrasena == password }
                            if (user != null) {
                                currentUser = user
                                isLoggedin = true
                                Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRegisterRequest = { email, password ->
                            if (users.any { it.email == email }) {
                                Toast.makeText(context, "El email ya está en uso", Toast.LENGTH_SHORT).show()
                            } else {
                                val isAdmin = email.endsWith("@admin.com")
                                val newUser = Usuario(email.split("@")[0], email, password, isAdmin)
                                users.add(newUser)
                                currentUser = newUser
                                isLoggedin = true
                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginApp(onLoginRequest: (String, String) -> Unit, onRegisterRequest: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistrando by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegistrando) "Registrarse" else "Iniciar sesión",
            fontSize = 24.sp,
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Email inválido", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.length < 4) {
                    Toast.makeText(context, "Contraseña inválida (min. 4 caracteres)", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isRegistrando) {
                    onRegisterRequest(email, password)
                } else {
                    onLoginRequest(email, password)
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegistrando) "Registrarse" else "Iniciar sesión")
        }
        TextButton(onClick = { isRegistrando = !isRegistrando }) {
            Text(if (isRegistrando) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
        }
    }
}

@Composable
fun PreludioApp(currentUser: Usuario, onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ANUNCIOS) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State lists for dynamic content
    val announcementPosts = remember { mutableStateListOf<Post>() }
    val communityPosts = remember { mutableStateListOf<Post>() }
    val eventItems = remember { mutableStateListOf<Evento>() }

    LaunchedEffect(Unit) {
        try {
            val history = NetworkUtils.api.getPosts()
            announcementPosts.clear()
            announcementPosts.addAll(history)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error fetching history: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        var socket: Socket? = null
        try {
            socket = IO.socket(NetworkUtils.BASE_URL)
            val gson = Gson()

            val onNewPost = io.socket.emitter.Emitter.Listener { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    try {
                        val newPost = gson.fromJson(data.toString(), Post::class.java)
                        scope.launch {
                            if (announcementPosts.none { it.id == newPost.id }) {
                                announcementPosts.add(0, newPost)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val onPostDeleted = io.socket.emitter.Emitter.Listener { args ->
                val data = args[0] as JSONObject
                val idToDelete = data.getString("id")
                scope.launch {
                    announcementPosts.removeAll { it.id == idToDelete }
                }
            }

            socket.on("new_post", onNewPost)
            socket.on("post_deleted", onPostDeleted)
            socket.connect()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            socket?.off("new_post")
            socket?.off("post_deleted")
            socket?.disconnect()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(topBar = {
            if (currentDestination != AppDestinations.PERFIL) {
                CenterAlignedTopAppBar(
                    title = { Text("EcoNews") },
                    actions = {
                        val showButton = when (currentDestination) {
                            AppDestinations.ANUNCIOS -> currentUser.isAdmin
                            AppDestinations.COMUNIDAD, AppDestinations.EVENTOS -> true
                            else -> false
                        }

                        if (showButton) {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Crear")
                            }
                        }
                    }
                )
            }
        }) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.ANUNCIOS -> AnunciosScreen(
                        user = currentUser,
                        posts = announcementPosts
                    )
                    AppDestinations.COMUNIDAD -> ComunidadScreen(
                        user = currentUser,
                        posts = communityPosts,
                        onDeletePost = { post -> communityPosts.remove(post) }
                    )
                    AppDestinations.SERVICIOS -> ServiciosScreen(user = currentUser)
                    AppDestinations.EVENTOS -> EventosScreen(
                        user = currentUser,
                        eventos = eventItems,
                        onDeleteEvent = { evento -> eventItems.remove(evento) }
                    )
                    AppDestinations.PERFIL -> PerfilScreen(user = currentUser, onLogout = onLogout)
                }
            }

            if (showCreateDialog) {
                CreatePostDialog(
                    destination = currentDestination,
                    onDismiss = { showCreateDialog = false },
                    onSubmit = { text, categoria ->
                        when (currentDestination) {
                            AppDestinations.COMUNIDAD -> {
                                val newPost = Post(
                                    id = System.currentTimeMillis().toString(),
                                    autor = currentUser.nombre,
                                    contenido = text,
                                    categoria = categoria,
                                    fecha = "Ahora"
                                )
                                communityPosts.add(0, newPost)
                            }
                            AppDestinations.EVENTOS -> {
                                val newEvent = Evento(
                                    id = System.currentTimeMillis().toInt(),
                                    titulo = text,
                                    fecha = "Ahora",
                                    descripcion = "Creado por ${currentUser.nombre}"
                                )
                                eventItems.add(0, newEvent)
                            }
                            AppDestinations.ANUNCIOS -> {
                                scope.launch {
                                    try {
                                        NetworkUtils.api.publishPost(PostBody(text))
                                        Toast.makeText(context, "Anuncio publicado", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al publicar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else -> {}
                        }
                        showCreateDialog = false
                    }
                )
            }
        }
    }
}


@Composable
fun AnunciosScreen(user: Usuario, posts: List<Post>){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)){
        item {Text("Anuncios oficiales", style = MaterialTheme.typography.titleMedium)}

        items(posts, key = {it.id}) { post ->
            Card(modifier = Modifier
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
                }
            }
        }
    }
}

@Composable
fun ComunidadScreen(user: Usuario, posts: List<Post>, onDeletePost: (Post) -> Unit){
    var filtro by remember { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Comida", "Servicios", "Materiales")
    val postsFiltrados = if (filtro == "Todos") posts else posts.filter { it.categoria == filtro }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categorias.forEach { card ->
                FilterChip(
                    selected = filtro == card,
                    onClick = { filtro = card },
                    label = { Text(card) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay publicaciones. ¡Crea una!")
            }
        } else {
            LazyColumn{
                items(postsFiltrados, key = { it.id }){ post ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                                Text(post.autor, style = MaterialTheme.typography.titleSmall)
                                SuggestionChip(onClick = {}, label = { Text(post.categoria) })
                            }
                            Text(post.contenido)
                            if (user.isAdmin || user.nombre == post.autor){
                                TextButton(onClick = { onDeletePost(post) }) { Text("ELIMINAR", color = Color.Red)}
                            }
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

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(16.dp)) {
        items(servicios){
            servicio ->
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .height(120.dp)
                        .clickable{servicioElegido = servicio},
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(servicio.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                        Text(servicio.nombre, style = MaterialTheme.typography.titleMedium)
                    }
                }
        }
    }
    if (servicioElegido != null)
        AlertDialog(
            onDismissRequest = {servicioElegido = null},
            icon = {Icon(servicioElegido!!.icon,"")},
            title = {Text(servicioElegido!!.nombre)},
            text = {Text(servicioElegido!!.info)},
            confirmButton = { TextButton(onClick = {servicioElegido = null}) { Text("Cerrar") }}
        )
}

@Composable
fun EventosScreen(user: Usuario, eventos: List<Evento>, onDeleteEvent: (Evento) -> Unit){
    if (eventos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay eventos. ¡Crea uno!")
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(eventos, key = { it.id }){ evento ->
                var isRegistered by rememberSaveable { mutableStateOf(evento.isRegistrado) }
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)){
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(evento.titulo, style = MaterialTheme.typography.titleLarge)
                        Text(evento.fecha, color = Color.Blue)
                        Text(evento.descripcion)
                        Spacer(modifier = Modifier.height(8.dp))

                        if(isRegistered){
                            Button(
                                onClick = { isRegistered = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) { Text("Cancelar Registro") }
                        }
                        else {
                            Button(onClick = { isRegistered = true })
                            { Text("Registrarse") }
                        }
                        if (user.isAdmin){
                            TextButton(onClick = { onDeleteEvent(evento) }) { Text("ELIMINAR", color = Color.Red)}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerfilScreen(user: Usuario, onLogout: () -> Unit){
    var bio by remember { mutableStateOf(user.bio) }
    var carrera by remember { mutableStateOf(user.carrera) }
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Text(user.nombre, style = MaterialTheme.typography.headlineMedium)
        Text(user.email, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = carrera,
            onValueChange = { carrera = it },
            label = { Text("Carrera") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / Estado") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            user.carrera = carrera
            user.bio = bio
            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
        }) {
            Text("Guardar cambios")
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onLogout) {
            Text("Cerrar Sesión", color = Color.Red)
        }
    }
}

@Composable
fun CreatePostDialog(destination: AppDestinations, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit){
    var text by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Materiales") }
    val categorias = listOf("Materiales", "Servicios", "Comida")
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val title = when(destination) {
                    AppDestinations.ANUNCIOS -> "Nuevo Anuncio"
                    AppDestinations.COMUNIDAD -> "Nueva Publicación"
                    AppDestinations.EVENTOS -> "Nuevo Evento"
                    else -> "Crear"
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                val label = if (destination == AppDestinations.EVENTOS) "Título del Evento" else "Contenido"
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                if(destination == AppDestinations.COMUNIDAD){
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Categoría:")

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         categorias.forEach { card ->
                            FilterChip(
                                selected = categoriaSeleccionada == card,
                                onClick = { categoriaSeleccionada = card },
                                label = { Text(card) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar")}
                    Button(onClick = {
                        if (text.isBlank()) {
                            Toast.makeText(context, "El contenido no puede estar vacío", Toast.LENGTH_SHORT).show()
                        } else {
                            onSubmit(text, categoriaSeleccionada)
                        }
                    }) { Text("Post") }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    ANUNCIOS("Anuncios", Icons.Default.Notifications),
    COMUNIDAD("Comunidad", Icons.Default.People),
    SERVICIOS("Servicios", Icons.Default.Build),
    EVENTOS("Eventos", Icons.Default.DateRange),
    PERFIL("Perfil", Icons.Default.AccountCircle),
}


data class PostBody(val text: String)

interface WahaApi {
    @GET("api/waha/posts")
    suspend fun getPosts(): List<Post>

    @POST("api/waha/publish")
    suspend fun publishPost(@Body body: PostBody): retrofit2.Response<Any>

    @DELETE("api/waha/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): retrofit2.Response<Any>
}

object NetworkUtils {
    const val BASE_URL = "http://10.0.2.2:3001/"

    val api: WahaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WahaApi::class.java)
    }
}
