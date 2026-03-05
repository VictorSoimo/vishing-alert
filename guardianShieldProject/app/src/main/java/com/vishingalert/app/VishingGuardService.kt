package com.vishingalert.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL

/**
 * The Vishing Guard Service handles the call initiation and alert receiving for the
 * pitch demonstration, relying on the remote Cloud Run service for analysis.
 * All logic is consolidated and simplified for stability and clarity.
 */
class VishingGuardService : Service() {

    // --- Configuration ---
    private val NOTIFICATION_CHANNEL_ID = "vishing_alert_channel"
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_ALERT_ID=2
    private val CLOUD_RUN_URL: String = "https://vishing-guard-analyzer-412589836588.us-central1.run.app"
    private var alertServerSocket: ServerSocket? = null
    // Coroutine scope for all asynchronous tasks (safe, non-blocking I/O)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // --- Service Overrides ---

    override fun onBind(intent: Intent?): IBinder? {
        // This is a started service, not a bound service.
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Start as a foreground service to guarantee it remains active.
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring Calls..."))
        Log.i(Companion.TAG, "Service created and promoted to Foreground. ")

        // Start the local server socket to listen for the fraud alert callback.
        startAlertListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This command is triggered by the UI when the user taps "Start Monitored Call."
        intent?.getStringExtra(Companion.ACTION_INITIATE_CALL)?.let {recipientNumber ->
            serviceScope.launch {
                // Initiates the HTTP POST request on a background thread
                initiateVishingCall(recipientNumber)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel all network coroutines
        stopAlertListener()
        stopForeground(true)
        Log.i(Companion.TAG, "Service destroyed. Resources cleaned up.")
    }

    // --- 1. Call Initiation (Outbound Network Request) ---

    /**
     * Sends the HTTP POST request to the Cloud Run Call Coordinator to start the conference bridge.
     */
    private suspend fun initiateVishingCall(recipientNumber: String): Boolean = withContext(Dispatchers.IO) {
        val initiateCallUrl = "$CLOUD_RUN_URL/api/call/initiate"
        Log.i(Companion.TAG, "Initiating call to $recipientNumber via Cloud Run: $initiateCallUrl")

        try {
            val url = URL(initiateCallUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonInputString = "{\"phoneNumber\": \"$recipientNumber\"}"

            connection.outputStream.use { os: OutputStream ->
                os.write(jsonInputString.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_OK) {
                Log.i(Companion.TAG, "Call initiation successful. Response code: $responseCode")
                return@withContext true
            } else {
                Log.e(Companion.TAG, "Call initiation failed. Response code: $responseCode")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(Companion.TAG, "Network error during call initiation: ${e.message}")
            return@withContext false
        }
    }

    // --- 2. Alert Listener (Inbound Network Server) ---

    /**
     * Starts a local HTTP server to listen for the fraud alert callback from the Cloud Run server.
     */
    private fun startAlertListener() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // The port must be 8081 to match the Ngrok tunnel configuration.
                alertServerSocket = ServerSocket(8081)
                delay(200L)
                Log.i(Companion.TAG, "Alert listener started on port 8081. Ready for Cloud Run callbacks.")

                while (isActive) {
                    val clientSocket = alertServerSocket!!.accept()
                    val inputStream = clientSocket.getInputStream()

                    val reader = inputStream.bufferedReader()
                    while (reader.readLine().also { if (it == null || it.isEmpty()) return@also } != null);



                    Log.w(Companion.TAG, "!!! ALERT RECEIVED VIA HTTP CALLBACK !!!")

                    withContext(Dispatchers.Main) {
                        triggerVibrationAlert()
                        showFraudAlertUI()
                    }
                        // Respond with HTTP 200 OK to the Cloud Run server/Ngrok
                    val outputStream: OutputStream = clientSocket.getOutputStream()
                    val response = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"
                    outputStream.write(response.toByteArray(Charsets.UTF_8))
                    outputStream.close()
                    clientSocket.close()
                }
            } catch (e: Exception) {
                if (e !is java.net.SocketException) {
                    Log.e(Companion.TAG, "Alert listener server error: ${e.message}")
                }
            } finally {
                Log.i(Companion.TAG, "Alert listener stopped.")
            }
        }
    }

    private fun stopAlertListener() {
        try {
            alertServerSocket?.close()
        } catch (e: Exception) {
            Log.e(Companion.TAG, "Error closing alert server socket: ${e.message}")
        }
    }
    private fun showFraudAlertUI() {

        val intent = Intent(this, AlertActivity::class.java).apply {
            // Flags required for an Activity launched from a Service to start a new task
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)

        // Use a simple Toast for immediate logging/feedback only
        Toast.makeText(this, "🚨 VISHING ATTACK DETECTED! Full-Screen Alert Launched! 🚨", Toast.LENGTH_SHORT).show()
        Log.i(Companion.TAG, "AlertActivity launched for screen takeover.")
    }

    private fun triggerVibrationAlert() {

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val pattern = longArrayOf(0,300,200, 300, 200, 300 ,500)
        val repeat = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, repeat)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }
        Log.w(Companion.TAG, "Vibration Alert triggered with disruptive pattern.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Vishing Guard Alert Channel",
                NotificationManager.IMPORTANCE_HIGH // MAX importance enables Head-Up Notifications (HUN)
            ).apply {
                description = "Critical alerts for Vishing attempts."
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 100, 500)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


    private fun buildNotification(contentText: String): Notification {
        // This builds the low-importance notification for the Foreground Service Status (ID 1)
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Vishing Guard Service Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Use LOW priority for the *status* notification
            .build()
    }


    // --- Companion Object (Purely for Constants) ---
    companion object {
        const val TAG = "VISHING_GUARD"
        const val ACTION_INITIATE_CALL = "com.vishingalert.app.INITIATE_CALL"
    }
}
