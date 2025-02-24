package com.corte2_firebase.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.app.PendingIntent;
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.corte2_firebase.MainActivity;
import com.corte2_firebase.R
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Mensaje recibido completo: $remoteMessage")

        // Verifica si el mensaje contiene datos
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Verifica si el mensaje contiene una notificación
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Notificación", it.body ?: "Mensaje")
        } ?: run {
            // Si no hay objeto de notificación, pero hay datos, también podemos mostrar una notificación
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "Notificación"
                val body = remoteMessage.data["body"] ?: "Tienes un nuevo mensaje"
                sendNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Aquí puedes enviar el token a tu servidor si es necesario
    }

    private fun sendNotification(title: String, messageBody: String) {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // Generar un ID aleatorio para evitar que los PendingIntent se sobrescriban
            val requestCode = Random().nextInt()

            // Crear PendingIntent compatible con todas las versiones de Android
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val channelId = "fcm_default_channel"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Configurar la notificación
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setLights(Color.RED, 3000, 3000)

            // Asegurarse de que el canal de notificación esté creado para Android Oreo y superior
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Canal principal",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Canal para notificaciones principales"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Canal de notificación creado: ${notificationManager.getNotificationChannel(channelId) != null}")
            }

            // Generar ID único para la notificación
            val notificationId = System.currentTimeMillis().toInt()

            // Mostrar la notificación usando NotificationManagerCompat
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, notificationBuilder.build())
                } else {
                    Log.d(TAG, "No hay permiso para mostrar notificaciones en Android 13+")
                }
            } else {
                notificationManager.notify(notificationId, notificationBuilder.build())
            }

            Log.d(TAG, "Notificación enviada con ID: $notificationId, Título: $title, Cuerpo: $messageBody")
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar notificación", e)
        }
    }
}