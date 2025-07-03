### INTEGRANTES
- Cordova Silva, Heidi Stephany
- Moroccoire Pacompia, Anthony Marcos

## ACTIVIDADES REALIZADAS
### 1. Configuración del Proyecto
- *Responsable*: Anthony y Heidi
- *Descripción*: Configuración inicial del proyecto Android con Kotlin y Jetpack Compose
- *Archivos*: 
  - build.gradle (configuración de dependencias)
  - settings.gradle (configuración de repositorios)
  - AndroidManifest.xml (permisos y configuración de la aplicación)

### 2. Implementación de la Interfaz de Usuario
- *Responsable*: Anthony
- *Descripción*: Codigo base para la interfaz del chat

### 3. Gestión de Estados y UI
- *Responsable*: Heidi
- *Descripción*: Implementación del manejo de estados en la UI
- *Funcionalidades*:
  - Estado de la conexión MQTT (conectado/desconectado)
  - Estado de los mensajes recibidos
  - Estado del campo de texto de entrada
  - Indicadores visuales de conectividad

### 4. Implementación del Cliente MQTT
- *Responsable*: Anthony
- *Descripción*: Código base para para el cliente MQTT usando Eclipse Paho
- *Clase*: MQTTManager
- *Funcionalidades*:
  - Conexión al broker MQTT
  - Publicación de mensajes
  - Suscripción a tópicos
  - Manejo de errores

### 5. Integración de Corrutinas
- *Responsable*: Heidi
- *Descripción*: Implementación de programación asíncrona con Kotlin Coroutines
- *Características*:
  - StateFlow para estados reactivos
  - Manejo asíncrono de conexiones MQTT
  - Ciclo de vida de la aplicación

### 6. Gestión de Conexiones y Reconexión
- *Responsable*: Anthony
- *Descripción*: Ajustes para el manejo de conexiones MQTT
- *Funcionalidades*:
  - Conexión automática al iniciar la app
  - Reconexión automática en caso de fallo

### 7. Configuración de Permisos
- *Responsable*: Heidi
- *Descripción*: Configuración de los permisos para la aplicación
- *Permisos implementados*:
  - INTERNET - Para conexiones de red
  - ACCESS_NETWORK_STATE - Para verificar estado de la red

### 8. Resolución de Conflictos de Dependencias
- *Responsable*: Anthony y Heidi
- *Descripción*: Solución de conflictos META-INF en el build
- *Soluciones implementadas*:
  - Exclusión de archivos duplicados en packaging.resources
  - Configuración de compatibilidad con Android 12+
  - Migración a HiveMQ Client por no tener dependencias deprecadas

### 9. Mejora de la interfaz
- *Responsable*: Heidi
- *Descripción*: Implementación del tema de la aplicación
- *Características*:
  - Uso de Material Design 3
  - Esquema de colores

### 10. Manejo del Ciclo de Vida
- *Responsable*: Anthony
- *Descripción*: Gestión del ciclo de vida de la aplicación
- *Métodos implementados*:
  - onCreate() - Inicialización y conexión
  - onResume() - Reconexión
  - onDestroy() - Limpieza de recursos al apagarse la aplicacion

### 12. Logging
- *Responsable*: Anthony y Heidi
- *Descripción*: Creación de logs
- *Características*:
  - Logging en el codigo para debuggear

## Configuración MQTT

- *Broker*: test.mosquitto.org
- *Puerto*: 1883
- *Tópico*: chat/publico/kotlin_compose
- *QoS*: AT_MOST_ONCE
- *Clean Session*: true
- *Keep Alive*: 60 segundos
