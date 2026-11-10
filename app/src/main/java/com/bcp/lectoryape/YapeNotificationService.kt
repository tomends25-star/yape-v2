package com.bcp.lectoryape

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Servicio que escucha las notificaciones del sistema de Android y filtra
 * exclusivamente las provenientes de la aplicación Yape (com.bcp.innovacxion.yapeapp).
 *
 * Cuando llega una notificación válida, procesa el texto mediante expresiones regulares
 * para extraer el remitente y monto, y finalmente lo reproduce en voz alta usando TextToSpeech.
 */
class YapeNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "YapeListener"
        private const val YAPE_PACKAGE_NAME = "com.bcp.innovacxion.yapeapp"

        /**
         * COMENTARIO IMPORTANTE:
         * El formato exacto del texto de la notificación de Yape puede variar entre versiones
         * de la app o actualizaciones de la pasarela del BCP (por ejemplo: "Paty te yapeó S/ 5",
         * "Paty te envió S/ 5.00", "Juan te yapeó S/. 20.00", etc.).
         *
         * Si Yape cambia la redacción de sus notificaciones, revise los registros emitidos en Logcat
         * bajo la etiqueta "YapeListener" y ajuste esta expresión regular según la estructura real observada.
         */
        private val YAPE_REGEX = Regex(
            """^([A-Za-zÁÉÍÓÚáéíóúñÑ\s]+?)\s+te\s+(?:yapeó|yapeo|envió|envio)\s+S\/?\.?\s*([\d]+(?:[.,]\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val pendingMessages = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "YapeNotificationService iniciado. Inicializando TextToSpeech...")
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localePE = Locale("es", "PE")
            val result = tts?.setLanguage(localePE)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Locale es_PE no disponible por defecto en el motor TTS. Probando con español genérico (es)...")
                val fallbackResult = tts?.setLanguage(Locale("es"))
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Idioma español no soportado en el motor TextToSpeech instalado.")
                } else {
                    Log.i(TAG, "TextToSpeech configurado con idioma español genérico.")
                }
            } else {
                Log.i(TAG, "TextToSpeech configurado exitosamente con Locale(es, PE).")
            }

            // Configurar velocidad y tono natural
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)

            isTtsInitialized = true

            // Reproducir cualquier mensaje que haya llegado mientras inicializaba el TTS
            synchronized(pendingMessages) {
                for (msg in pendingMessages) {
                    speakOut(msg)
                }
                pendingMessages.clear()
            }
        } else {
            Log.e(TAG, "Fallo al inicializar TextToSpeech. Código de estado: $status")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        val packageName = sbn.packageName

        // Filtro estricto: SOLO procesar notificaciones de Yape
        if (packageName != YAPE_PACKAGE_NAME) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extracción de campos EXTRA_TITLE, EXTRA_TEXT y EXTRA_BIG_TEXT
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_TEXT)
            ?: ""

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_BIG_TEXT)
            ?: ""

        // Registro detallado en Logcat con la etiqueta requerida "YapeListener" para depuración
        Log.d(TAG, "==================================================")
        Log.d(TAG, "Notificación detectada de Yape ($packageName)")
        Log.d(TAG, "Title: '$title'")
        Log.d(TAG, "Text: '$text'")
        Log.d(TAG, "BigText: '$bigText'")
        Log.d(TAG, "==================================================")

        // Determinar el contenido textual prioritario a evaluar (bigText si existe, sino text, sino title)
        val contentToParse = when {
            bigText.isNotBlank() -> bigText.trim()
            text.isNotBlank() -> text.trim()
            else -> title.trim()
        }

        if (contentToParse.isBlank()) {
            Log.w(TAG, "La notificación de Yape no contiene texto legible.")
            return
        }

        // Intento de extracción con la Expresión Regular
        val matchResult = YAPE_REGEX.find(contentToParse)
            ?: YAPE_REGEX.find(text.trim())
            ?: YAPE_REGEX.find(title.trim())

        val messageToSpeak: String
        if (matchResult != null) {
            val sender = matchResult.groupValues[1].trim()
            val rawAmount = matchResult.groupValues[2].trim()

            // Normalizar el monto a formato pronunciable (reemplazar coma por punto si es necesario)
            val formattedAmount = rawAmount.replace(",", ".")

            messageToSpeak = "Yape de $sender $formattedAmount soles"
            Log.i(TAG, "Regex coincidió exitosamente. Remitente: '$sender', Monto: '$formattedAmount'")
            Log.i(TAG, "Mensaje a vocalizar: '$messageToSpeak'")
        } else {
            // Si no hay coincidencia con la regex, se lee el texto completo tal cual
            messageToSpeak = contentToParse
            Log.w(TAG, "No hubo coincidencia con la regex. Se leerá el texto completo: '$messageToSpeak'")
        }

        speakOut(messageToSpeak)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // No se requiere acción al remover la notificación
    }

    private fun speakOut(message: String) {
        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS aún no inicializado. Encolando mensaje: $message")
            synchronized(pendingMessages) {
                pendingMessages.add(message)
            }
            return
        }

        val utteranceId = "yape_tts_${System.currentTimeMillis()}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "Destruyendo YapeNotificationService. Liberando recursos de TextToSpeech...")
        tts?.let {
            it.stop()
            it.shutdown()
        }
        tts = null
        isTtsInitialized = false
        super.onDestroy()
    }
}