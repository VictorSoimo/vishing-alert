package com.vishingalert.app.detector

import com.google.gson.Gson
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.ColorSpace
import android.util.Log
import android.util.Log.DEBUG
import android.util.Log.INFO
import com.vishingalert.app.model.VoskResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * Manages Vosk speech-to-text processing for real-time transcription
 * Handles 16kHz PCM audio input and outputs transcribed text
 */
class VoskSpeechToTextHandler(private val context: Context) {

    companion object {
        private const val TAG = "VoskSTTHandler"
        private const val SAMPLE_RATE = 16000
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"

    }

    private var recognizer: Recognizer? = null
    private var model = Model();
    private var isInitialized = false
    val gson = Gson()

    interface TranscriptionListener {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(error: String)
    }

    private var listener: TranscriptionListener? = null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            // Initialize Vosk library
            val level0 = null
            LibVosk.setLogLevel(level0) // Set appropriate log level

            // Load model (you'll need to add model files to assets)
            val modelPath = File(context.filesDir, MODEL_NAME)
            if (!modelPath.exists()) {
                Log.e(TAG, "Model not found at $modelPath")
                return@withContext false
            }

            model = Model("res/assets/vosk-model-small-en-us-0.15")
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())

            isInitialized = true
            Log.d(TAG, "Vosk STT initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk", e)
            listener?.onError("Initialization failed: ${e.message}")
            false
        }
    }

    suspend fun processPCMAudio(audioData: ByteArray) = withContext(Dispatchers.Default) {
        try {
            if (!isInitialized || recognizer == null) {
                Log.w(TAG, "Recognizer not initialized")
                return@withContext
            }

            // Feed audio data to recognizer
            if (recognizer!!.acceptWaveForm(audioData, audioData.size)) {
                // Final result received
                val result = recognizer!!.result
                Log.d(TAG, "Final result: $result")



                val text = extractTextFromResult(result)

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio", e)
            listener?.onError("Audio processing failed: ${e.message}")
        }
    }

    /**
     * Extract text from Vosk JSON result
     * Handles both partial ("result": []) and final ("result": "", "text": "") formats
     */
    private fun extractTextFromResult(jsonResult: String): VoskResult? {
        return try {

            val resultObject: VoskResult = gson.fromJson(
                jsonResult,           // The JSON String
                VoskResult::class.java // The target Class type
            )
            // Note: The 'text' field is directly accessible here
            // Example: println(resultObject.text)

            resultObject

        } catch (e: Exception) {
            // Handle JsonSyntaxException or other parsing errors
            e.printStackTrace()
            return null
        }
}




    fun setTranscriptionListener(listener: TranscriptionListener) {
        this.listener = listener
    }

    fun resetRecognizer() {
        try {
            recognizer?.reset()
            Log.d(TAG, "Recognizer reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting recognizer", e)
        }
    }

    fun release() {
        try {
            recognizer?.close()
            recognizer = null
            isInitialized = false
            Log.d(TAG, "Vosk resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
}
