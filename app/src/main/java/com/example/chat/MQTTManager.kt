package com.example.chat

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

open class MQTTManager(private val context: Context?) {

    private var mqttClient: Mqtt3AsyncClient? = null
    private val TAG = "MQTTManager"
    private val handler = Handler(Looper.getMainLooper())

    private val _isConnected = MutableStateFlow(false)
    open val isConnected: StateFlow<Boolean> = _isConnected

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    open val messages: StateFlow<List<String>> = _messages

    private val _connectionStatus = MutableStateFlow("Inicializando...")
    open val connectionStatus: StateFlow<String> = _connectionStatus

    // Configuración MQTT
    private val brokerHost = "test.mosquitto.org"
    private val brokerPort = 1883
    private val clientId = "AndroidClient_${UUID.randomUUID()}"
    private val chatTopic = "chat/publico/kotlin_compose"

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private var isConnecting = false

    init {
        Log.d(TAG, " Inicializando MQTTManager moderno con clientId: $clientId")
        initializeMqttClient()
    }

    private fun initializeMqttClient() {
        try {
            Log.d(TAG, " Creando cliente MQTT moderno")
            _connectionStatus.value = "Creando cliente MQTT..."

            mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildAsync()

            Log.d(TAG, " Cliente MQTT moderno inicializado")

        } catch (e: Exception) {
            Log.e(TAG, " Error al inicializar cliente MQTT: ${e.message}", e)
            _connectionStatus.value = "Error de inicialización: ${e.message}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    open fun connect() {
        val client = mqttClient
        if (client == null) {
            Log.e(TAG, " Cliente MQTT no inicializado")
            _connectionStatus.value = "Cliente no inicializado"
            return
        }

        if (isConnecting) {
            Log.d(TAG, " Ya hay una conexión en progreso")
            return
        }

        if (client.state.isConnected) {
            Log.d(TAG, " Ya conectado")
            _isConnected.value = true
            _connectionStatus.value = "Ya conectado"
            return
        }

        isConnecting = true
        Log.d(TAG, " Iniciando conexión MQTT...")
        _connectionStatus.value = "Conectando..."

        try {
            client.connectWith()
                .cleanSession(true)
                .keepAlive(60)
                .send()
                .orTimeout(15, TimeUnit.SECONDS)
                .whenComplete { connAck, throwable ->
                    isConnecting = false

                    if (throwable != null) {
                        Log.e(TAG, " Error al conectar: ${throwable.message}", throwable)
                        handler.post {
                            _isConnected.value = false
                            _connectionStatus.value = "Error: ${throwable.message}"
                        }

                        // Reintentar conexión
                        if (reconnectAttempts < maxReconnectAttempts) {
                            reconnectAttempts++
                            Log.d(TAG, "🔄 Reintentando conexión ($reconnectAttempts/$maxReconnectAttempts)")
                            handler.postDelayed({
                                connect()
                            }, 3000)
                        }
                    } else {
                        Log.d(TAG, " Conexión MQTT exitosa: ${connAck.returnCode}")
                        handler.post {
                            _isConnected.value = true
                            _connectionStatus.value = "Conectado exitosamente"
                            reconnectAttempts = 0
                        }

                        // Suscribirse al tópico
                        subscribeToTopic()
                    }
                }

        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, " Excepción al conectar: ${e.message}", e)
            handler.post {
                _isConnected.value = false
                _connectionStatus.value = "Excepción: ${e.message}"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    open fun disconnect() {
        val client = mqttClient
        if (client == null) {
            Log.w(TAG, " Cliente no inicializado")
            return
        }

        if (!client.state.isConnected) {
            Log.d(TAG, " No estaba conectado")
            _isConnected.value = false
            _connectionStatus.value = "No estaba conectado"
            return
        }

        try {
            Log.d(TAG, " Desconectando...")
            _connectionStatus.value = "Desconectando..."

            client.disconnect()
                .orTimeout(5, TimeUnit.SECONDS)
                .whenComplete { _, throwable ->
                    handler.post {
                        if (throwable != null) {
                            Log.e(TAG, " Error al desconectar: ${throwable.message}")
                            _connectionStatus.value = "Error de desconexión"
                        } else {
                            Log.d(TAG, " Desconectado exitosamente")
                            _connectionStatus.value = "Desconectado"
                        }
                        _isConnected.value = false
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, " Excepción al desconectar: ${e.message}", e)
            handler.post {
                _isConnected.value = false
                _connectionStatus.value = "Error de desconexión"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    open fun publish(message: String) {
        val client = mqttClient
        if (client == null || !client.state.isConnected) {
            Log.w(TAG, "No conectado - no se puede publicar")
            return
        }

        try {
            Log.d(TAG, " Publicando mensaje: '$message'")

            client.publishWith()
                .topic(chatTopic)
                .payload(message.toByteArray())
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_MOST_ONCE)
                .send()
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete { publish, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, " Error al publicar: ${throwable.message}")
                    } else {
                        Log.d(TAG, " Mensaje publicado exitosamente")
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, " Excepción al publicar: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun subscribeToTopic() {
        val client = mqttClient
        if (client == null || !client.state.isConnected) {
            Log.w(TAG, " No conectado - no se puede suscribir")
            return
        }

        try {
            Log.d(TAG, " Suscribiéndose a: $chatTopic")

            client.subscribeWith()
                .topicFilter(chatTopic)
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_MOST_ONCE)
                .callback { publish ->
                    val message = String(publish.payloadAsBytes)
                    if (message.isNotEmpty()) {
                        Log.d(TAG, " Mensaje recibido: '$message'")
                        handler.post {
                            _messages.value = _messages.value + message
                        }
                    }
                }
                .send()
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, " Error al suscribirse: ${throwable.message}")
                    } else {
                        Log.d(TAG, " Suscrito exitosamente a: $chatTopic")
                    }
                }

        } catch (e: Exception) {
            Log.e(TAG, " Excepción al suscribirse: ${e.message}", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    open fun resetConnection() {
        Log.d(TAG, " Reiniciando conexión...")
        reconnectAttempts = 0
        isConnecting = false
        disconnect()
        handler.postDelayed({
            connect()
        }, 2000)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun cleanup() {
        try {
            handler.removeCallbacksAndMessages(null)
            disconnect()

            // El cliente HiveMQ se limpia automáticamente
            mqttClient = null

            handler.post {
                _isConnected.value = false
                _connectionStatus.value = "Limpieza completada"
            }

            Log.d(TAG, " Cleanup completado")
        } catch (e: Exception) {
            Log.e(TAG, " Error en cleanup: ${e.message}", e)
        }
    }
}