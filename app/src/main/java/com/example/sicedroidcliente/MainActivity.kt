package com.example.sicedroidcliente

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientAppScreen()
        }
    }

    @Composable
    fun ClientAppScreen() {
        var resultText by remember { mutableStateOf("Esperando consulta...") }
        var inputMateria by remember { mutableStateOf("HACKEADO POR CLIENTE SICE") }
        var listaMaterias by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
        var mostrarLista by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val CONTENT_URI = Uri.parse("content://com.example.perfilsice.provider/alumnos")

        // Usamos una Column normal (SIN scroll) para que el LazyColumn de abajo pueda funcionar bien
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Cliente SICENET", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputMateria,
                onValueChange = { inputMateria = it },
                label = { Text("Nombre de la nueva materia falsa") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // FILA 1: BOTONES DE LECTURA (READ)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val jsonCrudo = withContext(Dispatchers.IO) { leerDatosCrudos(CONTENT_URI, "CARGA") }
                            if (jsonCrudo.startsWith("[")) {
                                listaMaterias = convertirJsonALista(jsonCrudo)
                                mostrarLista = true
                                resultText = "Mostrando Carga Académica:"
                            } else {
                                mostrarLista = false
                                resultText = jsonCrudo
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver Carga", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val jsonCrudo = withContext(Dispatchers.IO) { leerDatosCrudos(CONTENT_URI, "KARDEX") }
                            if (jsonCrudo.startsWith("[")) {
                                listaMaterias = convertirJsonALista(jsonCrudo)
                                mostrarLista = true
                                resultText = "Mostrando Kárdex:"
                            } else {
                                mostrarLista = false
                                resultText = jsonCrudo
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Ver Kárdex", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FILA 2: BOTONES DE ESCRITURA (WRITE)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val mensaje = withContext(Dispatchers.IO) { escribirDatos(CONTENT_URI, inputMateria, "CARGA") }
                            resultText = mensaje
                            mostrarLista = false // Ocultamos la lista para que lea el mensaje de éxito
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Mod Carga", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val mensaje = withContext(Dispatchers.IO) { escribirDatos(CONTENT_URI, inputMateria, "KARDEX") }
                            resultText = mensaje
                            mostrarLista = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Mod Kárdex", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // TEXTO DE RESULTADO
            Text(text = resultText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            // LISTA DESLIZABLE DE TARJETAS (Ocupa el resto de la pantalla hacia abajo)
            if (mostrarLista) {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(listaMaterias) { materiaDatos ->
                        TarjetaMateria(datos = materiaDatos)
                    }
                }
            }
        }
    }

    // COMPONENTE VISUAL PARA LAS TARJETAS
    @Composable
    fun TarjetaMateria(datos: Map<String, String>) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = datos["Materia"] ?: "Materia Desconocida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                datos.forEach { (llave, valor) ->
                    if (llave != "Materia" && valor.isNotBlank()) {
                        Text(text = "$llave: $valor", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // TRADUCTOR DE JSON A LISTA PARA LAS TARJETAS
    private fun convertirJsonALista(jsonString: String): List<Map<String, String>> {
        val lista = mutableListOf<Map<String, String>>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val objetoJson = jsonArray.getJSONObject(i)
                val mapaMateria = mutableMapOf<String, String>()
                val llaves = objetoJson.keys()

                while (llaves.hasNext()) {
                    val llave = llaves.next()
                    mapaMateria[llave] = objetoJson.getString(llave)
                }
                lista.add(mapaMateria)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    // FUNCIÓN DE LECTURA (READ)
    private fun leerDatosCrudos(uri: Uri, tipoConsulta: String): String {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            var jsonCrudo = ""

            if (cursor != null) {
                cursor.use {
                    if (it.moveToFirst()) {
                        jsonCrudo = if (tipoConsulta == "CARGA") {
                            it.getString(it.getColumnIndexOrThrow("cargaAcademicaRaw")) ?: "[]"
                        } else {
                            it.getString(it.getColumnIndexOrThrow("kardexRaw")) ?: "[]"
                        }
                    } else {
                        return "Base de datos vacía. Inicia sesión en PerfilSice primero."
                    }
                }
            } else {
                return "Error: Provider devolvió NULL."
            }
            jsonCrudo
        } catch (e: SecurityException) {
            "BLOQUEO DE SEGURIDAD (READ): ${e.message}"
        } catch (e: Exception) {
            "ERROR INTERNO: ${e.message}"
        }
    }

    // FUNCIÓN DE ESCRITURA (WRITE)
    private fun escribirDatos(uri: Uri, textoFalso: String, destino: String): String {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            var matriculaReal = ""

            if (cursor != null && cursor.moveToFirst()) {
                matriculaReal = cursor.getString(cursor.getColumnIndexOrThrow("matricula"))
                cursor.close()
            }

            if (matriculaReal.isEmpty()) return "FALLO: Base de datos vacía."

            val values = ContentValues()

            if (destino == "CARGA") {
                val jsonCarga = "[{ \"Materia\": \"$textoFalso\", \"Docente\": \"Hacker Anónimo\", \"Creditos\": \"5\" }]"
                values.put("cargaAcademicaRaw", jsonCarga)
            } else if (destino == "KARDEX") {
                val jsonKardex = "[{ \"Materia\": \"$textoFalso\", \"Calif\": \"100\", \"S1\": \"Aprobado\", \"Periodo\": \"Ago-Dic\" }]"
                values.put("kardexRaw", jsonKardex)
            }

            val rowsUpdated = contentResolver.update(uri, values, "matricula=?", arrayOf(matriculaReal))

            if (rowsUpdated > 0) {
                "¡ÉXITO! Se modificó $destino de $matriculaReal.\nPresiona 'Ver $destino' para comprobar."
            } else {
                "FALLO AL ACTUALIZAR."
            }
        } catch (e: SecurityException) {
            "BLOQUEO DE SEGURIDAD (WRITE): ${e.message}"
        } catch (e: Exception) {
            "CRASH AL ESCRIBIR: ${e.message}"
        }
    }
}