package com.vishingalert.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vishingalert.app.R
import com.vishingalert.app.detector.FraudDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for monitoring incoming SMS messages (smishing detection)
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val CHANNEL_ID = "vishing_monitoring_channel"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val fraudDetectionEngine = FraudDetectionEngine()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.displayOriginatingAddress
                val messageBody = message.messageBody
                
                Log.d(TAG, "SMS received from: $sender")
                
                // Analyze SMS for smishing indicators
                receiverScope.launch {
                    analyzeSmishingAttempt(context, sender, messageBody)
                }
            }
        }
    }

    private fun analyzeSmishingAttempt(context: Context, sender: String, messageBody: String) {
        val result = fraudDetectionEngine.analyzeText(messageBody)
        
        if (result.isSuspicious) {
            Log.w(TAG, "Suspicious SMS detected from $sender")
            showSmishingAlert(context, result, sender, messageBody)
        } else {
            Log.d(TAG, "SMS from $sender appears safe")
        }
    }

    private fun showSmishingAlert(
        context: Context,
        result: com.vishingalert.app.model.FraudAnalysisResult,
        sender: String,
        messageBody: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val indicators = result.detectedIndicators.joinToString(", ") { it.description }
        val truncatedMessage = if (messageBody.length > 100) {
            messageBody.substring(0, 97) + "..."
        } else {
            messageBody
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Suspicious SMS Detected")
            .setContentText("Potential smishing from $sender")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Message: $truncatedMessage\n\nFraud indicators: $indicators\nConfidence: ${(result.confidenceScore * 100).toInt()}%"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(sender.hashCode(), notification)
    }
}
