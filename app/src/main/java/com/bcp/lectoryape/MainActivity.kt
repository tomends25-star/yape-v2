package com.bcp.lectoryape

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla principal de Lector Yape.
 * Muestra el estado actual del permiso de "Acceso a notificaciones"
 * y permite al usuario abrir la pantalla de Ajustes del sistema para activarlo.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvPermissionStatus: TextView
    private lateinit var btnOpenSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnOpenSettings.setOnClickListener {
            // Abre la pantalla nativa de configuración del sistema para autorizar el servicio
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Comprobación de estado del permiso cada vez que la actividad vuelve al primer plano
        updatePermissionStatus()
    }

    /**
     * Verifica si el servicio de notificaciones YapeNotificationService está habilitado
     * por el usuario en la lista de "enabled_notification_listeners" de Android.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flatListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val names = flatListeners.split(":")
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null && componentName.packageName == pkgName) {
                return true
            }
        }
        return false
    }

    /**
     * Actualiza el texto del TextView según el estado del permiso.
     */
    private fun updatePermissionStatus() {
        val isEnabled = isNotificationServiceEnabled()

        if (isEnabled) {
            tvPermissionStatus.text = "Permiso: activado"
            tvPermissionStatus.setTextColor(Color.parseColor("#2E7D32")) // Verde oscuro
            btnOpenSettings.text = "Configuración de notificaciones"
        } else {
            tvPermissionStatus.text = "Permiso: no activado"
            tvPermissionStatus.setTextColor(Color.parseColor("#C62828")) // Rojo
            btnOpenSettings.text = "Activar acceso a notificaciones"
        }
    }
}