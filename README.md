# Lector Yape - App Android Nativa en Kotlin

Aplicación nativa de Android que escucha las notificaciones de Yape (`com.bcp.innovacxion.yapeapp`) y vocaliza en tiempo real las transferencias recibidas usando TextToSpeech con acento peruano (`Locale("es", "PE")`).

## Especificaciones Técnicas
- **Lenguaje:** Kotlin
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **UI:** Vistas tradicionales XML (View Binding / findViewById)
- **Servicio:** `NotificationListenerService` con permiso `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`
- **TTS:** `android.speech.tts.TextToSpeech` con `Locale("es", "PE")` y cola `QUEUE_ADD`

## Cómo compilar y ejecutar en Android Studio
1. Abre Android Studio (Giraffe, Hedgehog, Iguana o superior con JDK 17).
2. Haz clic en **File > Open...** y selecciona esta carpeta del proyecto.
3. Espera que Gradle descargue las dependencias y sincronice el proyecto.
4. Conecta tu teléfono Android por cable USB y activa **Depuración por USB** en Opciones de Desarrollador.
5. Haz clic en el botón verde **Run 'app'** (Shift + F10).
6. Una vez instalada la app, ábrela y presiona el botón **"Activar acceso a notificaciones"**.
7. En la lista de ajustes de Android, activa la casilla para **"Lector Yape"**.
8. ¡Listo! Cada vez que recibas un pago por Yape, tu teléfono anunciará automáticamente:
   *"Yape de [Nombre] [Monto] soles"*

## Ajuste de Batería en Xiaomi (MIUI/HyperOS) y Samsung
Para evitar que el sistema operativo cierre el servicio de notificaciones en segundo plano:
- **Xiaomi / Redmi / POCO:** Ve a Ajustes > Aplicaciones > Lector Yape > Habilita **Inicio automático** y en Ahorro de batería selecciona **"Sin restricciones"**.
- **Samsung:** Ve a Ajustes > Cuidado del dispositivo > Batería > Límites de uso en segundo plano > Añade Lector Yape a **"Aplicaciones que nunca se suspenden"**.
