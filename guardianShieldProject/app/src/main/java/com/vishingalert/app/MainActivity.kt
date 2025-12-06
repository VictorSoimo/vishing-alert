package com.vishingalert.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : Activity() {

    private val TAG = "MAIN_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views from the XML layout
        val phoneNumberInput = findViewById<EditText>(R.id.recipientPhoneNumber)
        val startCallButton = findViewById<Button>(R.id.startCallButton)
        val stopServiceButton = findViewById<Button>(R.id.stopServiceButton)

        // --- 1. Start Service and Trigger onStartCommand ---
        startCallButton.setOnClickListener {
            val recipientNumber = phoneNumberInput.text.toString().trim()

            if (recipientNumber.isEmpty() || !recipientNumber.startsWith("+")) {
                Toast.makeText(this, "Please enter a valid number (e.g., +1...).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Create the explicit Intent targeting the VishingGuardService
            val serviceIntent = Intent(this, VishingGuardService::class.java)

            // Attach the necessary data for the service to recognize the command
            serviceIntent.putExtra(VishingGuardService.ACTION_INITIATE_CALL, recipientNumber)

            Log.i(TAG, "Sending START_SERVICE intent with number: $recipientNumber")

            // Call startService(). This immediately triggers:
            // 1. VishingGuardService.onCreate() (if the service is not running)
            // 2. VishingGuardService.onStartCommand() (for every call, handling the initiation)
            startService(serviceIntent)

            Toast.makeText(this, "Attempting to initiate monitored call...", Toast.LENGTH_SHORT).show()
        }

        // --- 2. Stop Service ---
        stopServiceButton.setOnClickListener {
            val stopIntent = Intent(this, VishingGuardService::class.java)
            stopService(stopIntent)
            Toast.makeText(this, "Vishing Guard Service stopped.", Toast.LENGTH_SHORT).show()
        }
    }
}
