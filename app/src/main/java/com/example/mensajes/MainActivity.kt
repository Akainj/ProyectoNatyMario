package com.example.mensajes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mensajes.ui.theme.MensajesTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Mensaje(
    val id: Int? = null,
    val usuario: String,
    val texto: String
)

interface MensajeApi {
    @GET("mensajes")
    suspend fun obtenerMensajes(): List<Mensaje>

    @POST("mensajes")
    suspend fun enviarMensaje(@Body mensaje: Mensaje): Mensaje
}
object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    val api: MensajeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MensajeApi::class.java)
    }
}

// MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MensajesTheme {
                PantallaPrincipal()
            }
        }
    }
}

@Composable
fun PantallaPrincipal() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            PantallaLogin(navController)
        }
        composable("chat/{usuario}") { backStackEntry ->
            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
            PantallaMensajes(usuario, navController)
        }
    }
}

@Composable
fun PantallaLogin(navController: NavController) {
    var nombre by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = contraseña,
            onValueChange = { contraseña = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val usuarios = listOf("Mario", "Natalia", "Israel")
                if (usuarios.contains(nombre) && contraseña == "123") {
                    navController.navigate("chat/$nombre")
                } else {
                    error = "Usuario o contraseña incorrectos"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("INGRESAR")
        }

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PantallaMensajes(usuarioActual: String, navController: NavController) {
    var mensajes by remember { mutableStateOf(listOf<Mensaje>()) }
    var textoNuevo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val api = RetrofitInstance.api

    fun cargarMensajes() {
        scope.launch {
            try {
                mensajes = api.obtenerMensajes()
                error = ""
            } catch (e: Exception) {
                error = "Error al cargar mensajes: ${e.message}"
            }
        }
    }
    LaunchedEffect(Unit) {
        cargarMensajes()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Usuario: $usuarioActual",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre los botones
            ) {
                Button(
                    onClick = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("◀️")
                }
                Button(onClick = { cargarMensajes() }) {
                    Text("Recargar")
                }
            }
        }
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(mensajes) { mensaje ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = mensaje.usuario,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = mensaje.texto,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextField(
                value = textoNuevo,
                onValueChange = { textoNuevo = it },
                label = { Text("Escribe un mensaje") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (textoNuevo.isNotEmpty()) {
                    scope.launch {
                        try {
                            api.enviarMensaje(
                                Mensaje(
                                    usuario = usuarioActual,
                                    texto = textoNuevo
                                )
                            )
                            textoNuevo = ""
                            cargarMensajes()
                            error = ""
                        } catch (e: Exception) {
                            error = "Error al enviar: ${e.message}"
                        }
                    }
                }
            }) {
                Text("Enviar")
            }
        }
    }
}