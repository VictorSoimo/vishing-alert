package com.example.vishingalert.voip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Captures raw audio from the microphone during VoIP call
 * Feeds audio to STT and threat detection in real-time
 */
class CallAudioCaptureService(private val context: Context) {

    companion object {
        private const val TAG = "CallAudioCapture"
        private const val SAMPLE_RATE = 16000 // 16kHz
        private const val CHANNELS = 1 // Mono
        private const val BITS_PER_SAMPLE = 16
        private const val BUFFER_SIZE_MS = 100 // 100ms chunks
    }

    interface AudioFrameCallback {
        fun onAudioFrame(audioData: ByteArray, sampleRate: Int, channels: Int)
        fun onError(error: String)
    }

    private var audioRecord: AudioRecord? = null
    private var capturingJob: Job? = null
    private var callback: AudioFrameCallback? = null
    private var isCapturing = false

    /**
     * Start capturing audio from microphone (during active call)
     */
    suspend fun startCapture() = withContext(Dispatchers.IO) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,  // You need to pass Context, not 'this'
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "RECORD_AUDIO permission not granted")
                callback?.onError("Permission denied: RECORD_AUDIO")
                return@withContext
            }
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )


            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            audioRecord?.startRecording()
            isCapturing = true

            Log.d(TAG, "Audio capture started (sample_rate=$SAMPLE_RATE)")

            // Start continuous capture loop
            startCapturingLoop(bufferSize)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
            callback?.onError("Audio capture failed: ${e.message}")
            isCapturing = false
        }
    }

    /**
     * Continuous loop to read audio frames from microphone
     */
    private suspend fun startCapturingLoop(bufferSize: Int) = withContext(Dispatchers.Default) {
        capturingJob = launch {
            val audioBuffer = ByteArray(bufferSize)

            while (isActive && isCapturing && audioRecord != null) {
                try {
                    val readSize = audioRecord!!.read(audioBuffer, 0, bufferSize)

                    if (readSize > 0) {
                        // Send audio frame to callback (STT, threat detection, etc.)
                        val frameData = audioBuffer.copyOf(readSize)
                        callback?.onAudioFrame(frameData, SAMPLE_RATE, CHANNELS)

                        Log.d(TAG, "Audio frame captured: ${readSize} bytes")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error reading audio", e)
                    callback?.onError("Audio read error: ${e.message}")
                    break
                }
            }
        }
    }

    /**
     * Stop audio capture
     */
    suspend fun stopCapture() = withContext(Dispatchers.IO) {
        try {
            isCapturing = false
            capturingJob?.cancel()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Audio capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        }
    }

    fun setAudioFrameCallback(callback: AudioFrameCallback) {
        this.callback = callback
    }
}
