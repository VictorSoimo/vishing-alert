package com.vishingalert.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vishingalert.app.R
import com.vishingalert.app.detector.FraudDetectionEngine
import com.vishingalert.app.detector.SpeechToTextHandler
import com.vishingalert.app.model.FraudAnalysisResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service for monitoring calls and detecting vishing attempts
 */
class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "CallMonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vishing_monitoring_channel"
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var fraudDetectionEngine: FraudDetectionEngine
    private var speechToTextHandler: SpeechToTextHandler? = null
    private var isMonitoring = false
    private val accumulatedTranscript = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        fraudDetectionEngine = FraudDetectionEngine()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        if (isMonitoring) return

        Log.d(TAG, "Starting monitoring")
        isMonitoring = true

        // Start foreground service
        val notification = createNotification("Monitoring active", "Protecting you from fraud")
        startForeground(NOTIFICATION_ID, notification)

        // Initialize speech-to-text
        speechToTextHandler = SpeechToTextHandler(applicationContext)
        
        // Note: Actual call audio capture requires special system permissions
        // and is restricted on Android. This is a simplified implementation.
        // In a production app, you would need:
        // 1. System-level access or OEM partnership
        // 2. Accessibility Service for capturing call audio
        // 3. Custom ROM modifications
        
        Log.d(TAG, "Monitoring started successfully")
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return

        Log.d(TAG, "Stopping monitoring")
        isMonitoring = false

        speechToTextHandler?.release()
        speechToTextHandler = null
        accumulatedTranscript.clear()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Starts analyzing call audio (would be triggered by phone state changes)
     */
    fun startCallAnalysis(phoneNumber: String) {
        if (!isMonitoring) return

        Log.d(TAG, "Starting call analysis for $phoneNumber")
        accumulatedTranscript.clear()

        speechToTextHandler?.startListening { transcribedText ->
            serviceScope.launch {
                processTranscription(transcribedText, phoneNumber)
            }
        }
    }

    /**
     * Processes transcribed text and checks for fraud
     */
    private fun processTranscription(text: String, phoneNumber: String) {
        accumulatedTranscript.append(" ").append(text)
        
        // Analyze the accumulated transcript
        val result = fraudDetectionEngine.analyzeText(accumulatedTranscript.toString())
        
        if (result.isSuspicious) {
            Log.w(TAG, "Suspicious activity detected in call from $phoneNumber")
            showFraudAlert(result, phoneNumber)
        }
    }

    /**
     * Shows a notification when fraud is detected
     */
    private fun showFraudAlert(result: FraudAnalysisResult, phoneNumber: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val indicators = result.detectedIndicators.joinToString(", ") { it.description }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.fraud_alert_title))
            .setContentText("Suspicious call detected from $phoneNumber")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Fraud indicators: $indicators\nConfidence: ${(result.confidenceScore * 100).toInt()}%"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(phoneNumber.hashCode(), notification)
    }

    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        speechToTextHandler?.release()
        serviceScope.cancel()
    }
}
