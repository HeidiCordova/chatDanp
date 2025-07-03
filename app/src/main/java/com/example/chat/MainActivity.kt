package com.example.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.chat.ui.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mqttManager: MQTTManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el MQTTManager
        mqttManager = MQTTManager(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val view = LocalView.current

            // Configurar barra de estado
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }

            ChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleChatScreen(mqttManager = mqttManager)
                }
            }
        }

        // Intentar conectar al broker cuando se crea la actividad
        lifecycleScope.launch {
            mqttManager.connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.cleanup()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (!mqttManager.isConnected.value) {
                mqttManager.connect()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(mqttManager: MQTTManager) {
    // Observar estado de conexión y mensajes del MQTTManager
    val isConnected by mqttManager.isConnected.collectAsState()
    val messages by mqttManager.messages.collectAsState()
    val connectionStatus by mqttManager.connectionStatus.collectAsState()
    var messageInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat MQTT") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Botón de reconexión
                    IconButton(
                        onClick = { mqttManager.resetConnection() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconectar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Indicador de estado
                    Card(
                        modifier = Modifier.padding(end = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected)
                                Color.Green.copy(alpha = 0.2f)
                            else
                                Color.Red.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = if (isConnected) "● Conectado" else "● Desconectado",
                            color = if (isConnected) Color.Green else Color.Red,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de estado de conexión
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Estado: $connectionStatus",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Lista de mensajes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No hay mensajes aún.\n${if (isConnected) "¡Envía el primer mensaje!" else "Esperando conexión..."}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(messages.reversed()) { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            // Área de entrada de texto y botón de envío
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Escribe un mensaje...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = isConnected,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            mqttManager.publish(messageInput)
                            messageInput = ""
                        }
                    },
                    enabled = isConnected && messageInput.isNotBlank()
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// Mock MQTTManager para preview
class MockMQTTManager : MQTTManager(null) {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val messages: StateFlow<List<String>> = MutableStateFlow(
        listOf(
            "¡Hola a todos!",
            "Este es un mensaje de prueba.",
            "¡Bienvenido al chat!"
        )
    )
    override val connectionStatus: StateFlow<String> = MutableStateFlow("Conectado")
    override fun connect() {}
    override fun disconnect() {}
    override fun publish(message: String) {}
    override fun resetConnection() {}
}

@Preview(showBackground = true)
@Composable
fun PreviewChatScreen() {
    ChatTheme {
        SimpleChatScreen(mqttManager = MockMQTTManager())
    }
}