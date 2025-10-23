package com.vishingalert.app.detector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * On-device Speech-to-Text handler using Android SpeechRecognizer
 * Provides real-time transcription without cloud processing
 */
class SpeechToTextHandler(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToTextHandler"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var transcriptionCallback: ((String) -> Unit)? = null

    /**
     * Starts listening for speech
     * @param onTranscription Callback invoked with transcribed text
     */
    fun startListening(onTranscription: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }

        transcriptionCallback = onTranscription
        
        // Clean up existing recognizer
        stopListening()
        
        // Create new speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Use on-device recognition if available
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        isListening = true
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Started listening for speech")
    }

    /**
     * Stops listening for speech
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening for speech")
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Creates a recognition listener for handling speech recognition events
     */
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed - can be used for visualization
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            Log.e(TAG, "Recognition error: $errorMessage")
            
            // Restart listening if it's a temporary error
            if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                // Optionally restart listening for continuous monitoring
            }
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val transcribedText = matches[0]
                    Log.d(TAG, "Transcribed: $transcribedText")
                    transcriptionCallback?.invoke(transcribedText)
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Handle partial results for real-time processing
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partialText = matches[0]
                    Log.d(TAG, "Partial: $partialText")
                    // Optionally process partial results for early detection
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "Event: $eventType")
        }
    }

    /**
     * Checks if speech recognition is available on the device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Releases resources
     */
    fun release() {
        stopListening()
        transcriptionCallback = null
    }
}
