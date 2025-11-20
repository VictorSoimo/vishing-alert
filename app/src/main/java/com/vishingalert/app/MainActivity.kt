package com.vishingalert.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vishingalert.app.service.CallProcessingService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var callService: CallProcessingService
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var logsButton: Button
    private lateinit var logOutput: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()
        callService = CallProcessingService(this)

        requestPermissions()
    }

    private fun initializeUI() {
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        logsButton = findViewById(R.id.logs_button)
        logOutput = findViewById(R.id.log_output)

        startButton.setOnClickListener {
            startCall()
        }

        stopButton.setOnClickListener {
            stopCall()
        }

        logsButton.setOnClickListener {
            displayLogs()
        }
    }

    private fun startCall() {
        lifecycleScope.launch {
            // Replace with your actual Twilio token and room details
            val token = "YOUR_TWILIO_TOKEN"
            val roomName = "test-room"
            val participantName = "test-participant"

            callService.startCallProcessing(token, roomName, participantName)
        }
    }

    private fun stopCall() {
        lifecycleScope.launch {
            callService.stopCallProcessing()
        }
    }

    private fun displayLogs() {
        val log = callService.getThreatLog()
        val stats = callService.getStatistics()

        logOutput.text = """
            === THREAT LOG ===
            $log

            === STATISTICS ===
            Calls Monitored: ${stats["calls_monitored"]}
            Threats Detected: ${stats["threats_detected"]}
        """.trimIndent()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                logOutput.text = "Some permissions were denied"
            }
        }
    }
}
