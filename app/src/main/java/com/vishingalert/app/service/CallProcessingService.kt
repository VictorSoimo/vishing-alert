package com.vishingalert.app.service

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.vishingalert.app.detector.VoskSpeechToTextHandler
import com.vishingalert.app.detector.ThreatDetectionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coordinates the entire pipeline: audio capture → STT → threat detection → alerting
 * Handles logging and user feedback
 */
class CallProcessingService(private val context: Context) {

    companion object {
        private const val TAG = "CallProcessingService"
        private const val PREF_NAME = "vishing_alert_prefs"
        private const val THREAT_LOG_KEY = "threat_log"
        private const val CALL_COUNT_KEY = "call_count"
        private const val THREAT_COUNT_KEY = "threat_count"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val callService = WebRTCCallService(context)
    private val sttHandler = VoskSpeechToTextHandler(context)
    private val threatEngine = ThreatDetectionEngine()

    private var processingJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var currentTranscription = StringBuilder()

    /**
     * Initialize and start processing a VoIP call
     */
    suspend fun startCallProcessing(
        token: String,
        roomName: String,
        participantName: String
    ) = coroutineScope {
        try {
            // Initialize STT
            val sttReady = sttHandler.initialize()
            if (!sttReady) {
                Log.e(TAG, "Failed to initialize STT")
                showAlert("STT initialization failed")
                return@coroutineScope false
            }

            // Set up STT listener
            sttHandler.setTranscriptionListener(object : VoskSpeechToTextHandler.TranscriptionListener {
                override fun onPartialResult(text: String) {
                    // Update UI with partial results (optional)
                    Log.d(TAG, "Partial: $text")
                }

                override fun onFinalResult(text: String) {
                    currentTranscription.append(text).append(" ")

                    // Analyze for threats
                    analyzeForThreats(text)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "STT Error: $error")
                    logEvent("STT_ERROR", error)
                }
            })

            // Set up audio capture listener
            callService.setAudioCaptureListener(object : WebRTCCallService.AudioCaptureListener {
                override fun onAudioFrame(audioData: ByteArray, sampleRate: Int, channels: Int) {
                    // Feed audio to STT
                    processingJob?.cancel() // Cancel previous job
                    processingJob = launch(Dispatchers.IO) {
                        sttHandler.processPCMAudio(audioData)
                    }
                }

                override fun onCallConnected(participantName: String) {
                    Log.d(TAG, "Call connected with $participantName")
                    logEvent("CALL_CONNECTED", participantName)
                    incrementCallCount()
                }

                override fun onCallDisconnected() {
                    Log.d(TAG, "Call disconnected")
                    logEvent("CALL_DISCONNECTED", currentTranscription.toString())
                }

                override fun onError(error: String) {
                    Log.e(TAG, "Audio capture error: $error")
                    logEvent("AUDIO_ERROR", error)
                }
            })

            // Connect to Twilio room
            val connected = callService.connectToRoom(token, roomName, participantName)
            if (!connected) {
                Log.e(TAG, "Failed to connect to call")
                return@coroutineScope false
            }

            // Start audio capture
            callService.startLocalAudioCapture()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call processing", e)
            logEvent("PROCESSING_ERROR", e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Analyze transcribed text for threats
     */
    private fun analyzeForThreats(text: String) {
        val result = threatEngine.analyzeText(text)

        if (result.isThreat) {
            Log.w(TAG, "THREAT DETECTED: ${result.threatSummary}")

            // Increment threat counter
            incrementThreatCount()

            // Alert user
            showAlert(result.threatSummary)

            // Vibrate
            triggerVibrationAlert()

            // Log threat
            logThreatDetection(text, result)
        }
    }

    /**
     * Show alert to user (Toast or notification)
     */
    private fun showAlert(message: String) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "🚨 THREAT: $message", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert", e)
        }
    }

    /**
     * Vibrate phone as alert
     */
    private fun triggerVibrationAlert() {
        try {
            // Pattern: [delay, vibrate, delay, vibrate]
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            vibrator.vibrate(pattern, -1) // -1 means don't repeat
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    /**
     * Log threat detection to SharedPreferences
     */
    private fun logThreatDetection(
        transcribedText: String,
        result: ThreatDetectionEngine.ThreatAnalysisResult
    ) {
        val timestamp = dateFormat.format(Date())
        val threatEntry = """
            [$timestamp]
            Text: $transcribedText
            Threat Level: ${String.format("%.2f", result.threatLevel)}
            Summary: ${result.threatSummary}
            Keywords: ${result.detectedKeywords.map { it.phrase }.joinToString(", ")}
            ---
        """.trimIndent()

        val currentLog = prefs.getString(THREAT_LOG_KEY, "") ?: ""
        val updatedLog = currentLog + "\n" + threatEntry

        prefs.edit().putString(THREAT_LOG_KEY, updatedLog).apply()

        Log.d(TAG, "Threat logged: $threatEntry")
    }

    /**
     * Log generic event
     */
    private fun logEvent(eventType: String, details: String) {
        val timestamp = dateFormat.format(Date())
        Log.d(TAG, "[$timestamp] $eventType: $details")
    }

    /**
     * Increment call counter
     */
    private fun incrementCallCount() {
        val count = prefs.getInt(CALL_COUNT_KEY, 0)
        prefs.edit().putInt(CALL_COUNT_KEY, count + 1).apply()
    }

    /**
     * Increment threat counter
     */
    private fun incrementThreatCount() {
        val count = prefs.getInt(THREAT_COUNT_KEY, 0)
        prefs.edit().putInt(THREAT_COUNT_KEY, count + 1).apply()
    }

    /**
     * Get threat log from SharedPreferences
     */
    fun getThreatLog(): String {
        return prefs.getString(THREAT_LOG_KEY, "No threats logged") ?: "No threats logged"
    }

    /**
     * Get statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "calls_monitored" to prefs.getInt(CALL_COUNT_KEY, 0),
            "threats_detected" to prefs.getInt(THREAT_COUNT_KEY, 0)
        )
    }

    /**
     * Clear all logs
     */
    fun clearLogs() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Logs cleared")
    }

    /**
     * Stop call processing
     */
    suspend fun stopCallProcessing() = withContext(Dispatchers.Main) {
        try {
            processingJob?.cancel()
            callService.stopAudioCapture()
            callService.disconnectCall()
            sttHandler.release()
            Log.d(TAG, "Call processing stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping call processing", e)
        }
    }
}
