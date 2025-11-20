package com.example.vishingalert.service

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.vishingalert.voip.CallAudioCaptureService
import com.vishingalert.app.detector.VoskSpeechToTextHandler
import com.vishingalert.app.detector.ThreatDetectionEngine
import com.example.vishingalert.voip.WebRTCSignalingManager
import com.example.vishingalert.voip.VoIPCallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Integrates VoIP call, audio capture, STT, and threat detection
 * Real-time analysis during active call
 */
class VoIPThreatAnalysisService(private val context: Context) {

    companion object {
        private const val TAG = "VoIPThreatAnalysis"
    }

    private val audioCapture = CallAudioCaptureService(context)
    private val threatEngine = ThreatDetectionEngine()
    private lateinit var sttHandler: VoskSpeechToTextHandler
    private var callManager: VoIPCallManager? = null

    /**
     * Initialize for VoIP call analysis
     */
    suspend fun initialize(roomId: String, localUserId: String): Boolean = withContext(Dispatchers.Main) {
        try {
            // Initialize STT
            sttHandler = VoskSpeechToTextHandler(context)
            val sttReady = sttHandler.initialize()
            if (!sttReady) {
                Log.e(TAG, "STT initialization failed")
                return@withContext false
            }

            // Set up STT to process transcribed text for threats
            sttHandler.setTranscriptionListener(object : VoskSpeechToTextHandler.TranscriptionListener {
                override fun onPartialResult(text: String) {
                    Log.d(TAG, "Partial transcription: $text")
                }

                override fun onFinalResult(text: String) {
                    // Analyze for threats
                    analyzeThreat(text)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "STT error: $error")
                }
            })

            // Initialize WebRTC call manager
            callManager = VoIPCallManager(context, roomId, localUserId)
            val callReady = callManager!!.initialize()
            if (!callReady) {
                Log.e(TAG, "Call manager initialization failed")
                return@withContext false
            }

            // Set up audio capture to feed to STT
            audioCapture.setAudioFrameCallback(object : CallAudioCaptureService.AudioFrameCallback {
                override fun onAudioFrame(audioData: ByteArray, sampleRate: Int, channels: Int) {
                    // Feed audio to STT
                    launch(Dispatchers.IO) {
                        sttHandler.processPCMAudio(audioData)
                    }
                }

                override fun onError(error: String) {
                    Log.e(TAG, "Audio capture error: $error")
                }
            })

            Log.d(TAG, "VoIP Threat Analysis Service initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error", e)
            false
        }
    }

    /**
     * Start a new VoIP call (as caller)
     */
    suspend fun startCall() {
        try {
            // Start audio capture
            audioCapture.startCapture()

            // Initiate call
            callManager?.startCall()

            Log.d(TAG, "Call started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call", e)
        }
    }

    /**
     * Join an existing VoIP call (as callee)
     */
    suspend fun joinCall() {
        try {
            // Start audio capture
            audioCapture.startCapture()

            // Join call
            callManager?.joinCall()

            Log.d(TAG, "Joined call")
        } catch (e: Exception) {
            Log.e(TAG, "Error joining call", e)
        }
    }

    /**
     * Analyze transcribed text for threats
     */
    private suspend fun analyzeThreat(text: String) {
        val result = threatEngine.analyzeText(text)

        if (result.isThreat) {
            Log.w(TAG, "🚨 THREAT DETECTED: ${result.threatSummary}")

            // Alert user immediately
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "🚨 THREAT: ${result.threatSummary}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * End the call and clean up
     */
    suspend fun endCall() {
        try {
            audioCapture.stopCapture()
            callManager?.endCall()
            sttHandler.release()
            Log.d(TAG, "Call ended")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
    }
}
