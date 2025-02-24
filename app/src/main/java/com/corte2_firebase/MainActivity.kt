package com.corte2_firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.corte2_firebase.ui.theme.Corte2firebaseTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    // Usando el nuevo sistema de permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        var fcmToken by mutableStateOf("")

        // Inicializar Firebase Cloud Messaging y solicitar token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Obtener el token
            val token = task.result
            fcmToken = token

            // Log y notificación local con el token
            Log.d(TAG, "FCM Token: $token")
            Toast.makeText(baseContext, "Token FCM obtenido", Toast.LENGTH_SHORT).show()
        })

        // Opcional: Suscribirse a un tema
        FirebaseMessaging.getInstance().subscribeToTopic("noticias")
            .addOnCompleteListener { task ->
                var msg = "Suscripción correcta al tema"
                if (!task.isSuccessful) {
                    msg = "Fallo en la suscripción al tema"
                }
                Log.d(TAG, msg)
            }

        setContent {
            Corte2firebaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FCMTokenScreen(fcmToken, onSendTestNotification = { sendTestNotification() })
                }
            }
        }
    }

    private fun sendTestNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "fcm_default_channel"

            // Crear canal si es necesario (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Canal de prueba",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Canal para notificaciones de prueba"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Canal de notificación creado para prueba: $channelId")
            }

            // Construir la notificación
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Notificación de prueba")
                .setContentText("Esta es una notificación de prueba local")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Mostrar la notificación
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())

            Log.d(TAG, "Notificación de prueba enviada con ID: $notificationId")
            Toast.makeText(this, "Notificación de prueba enviada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar notificación de prueba", e)
            Toast.makeText(this, "Error al enviar notificación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FCMTokenScreen(token: String, onSendTestNotification: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Firebase Cloud Messaging",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (token.isNotEmpty()) {
            Text(
                text = "Token FCM:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = token,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("FCM Token", token)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Token copiado al portapapeles", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copiar Token")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSendTestNotification
            ) {
                Text("Enviar notificación de prueba")
            }
        } else {
            CircularProgressIndicator()
            Text(
                text = "Obteniendo token FCM...",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}