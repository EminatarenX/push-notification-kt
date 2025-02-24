package com.corte2_firebase

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Crea canales de notificación para Android 8.0 (Oreo) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "fcm_default_channel"
            val channelName = "Notificaciones Push"
            val channelDescription = "Canal para recibir notificaciones push de la aplicación"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }

            // Registra el canal con el sistema
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("MyApplication", "Canal de notificación creado: $channelId")
        }
    }
}