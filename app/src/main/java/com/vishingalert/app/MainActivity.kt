package com.vishingalert.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.vishingalert.app.service.CallMonitoringService
import com.vishingalert.app.util.PermissionUtil
import com.vishingalert.app.util.PreferenceManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Activity - Entry point of the application
 */
class MainActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var statusTextView: TextView
    private lateinit var toggleButton: MaterialButton
    private lateinit var callsMonitoredTextView: TextView
    private lateinit var threatsDetectedTextView: TextView
    private lateinit var lastScanTextView: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            updateUI()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
            showPermissionExplanation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferenceManager = PreferenceManager(this)

        initializeViews()
        setupListeners()
        updateUI()

        // Show permission dialog on first launch
        if (preferenceManager.isFirstLaunch) {
            showPermissionExplanation()
            preferenceManager.isFirstLaunch = false
        }
    }

    private fun initializeViews() {
        statusTextView = findViewById(R.id.statusTextView)
        toggleButton = findViewById(R.id.toggleMonitoringButton)
        callsMonitoredTextView = findViewById(R.id.callsMonitoredTextView)
        threatsDetectedTextView = findViewById(R.id.threatsDetectedTextView)
        lastScanTextView = findViewById(R.id.lastScanTextView)
    }

    private fun setupListeners() {
        toggleButton.setOnClickListener {
            if (preferenceManager.isMonitoringEnabled) {
                stopMonitoring()
            } else {
                if (PermissionUtil.areAllPermissionsGranted(this)) {
                    startMonitoring()
                } else {
                    requestPermissions()
                }
            }
        }
    }

    private fun updateUI() {
        val isMonitoring = preferenceManager.isMonitoringEnabled

        statusTextView.text = if (isMonitoring) {
            getString(R.string.monitoring_active)
        } else {
            getString(R.string.monitoring_inactive)
        }

        statusTextView.setTextColor(
            ContextCompat.getColor(
                this,
                if (isMonitoring) R.color.success else R.color.text_secondary
            )
        )

        toggleButton.text = if (isMonitoring) {
            getString(R.string.disable_monitoring)
        } else {
            getString(R.string.enable_monitoring)
        }

        // Update statistics
        callsMonitoredTextView.text = getString(
            R.string.calls_monitored,
            preferenceManager.callsMonitored
        )

        threatsDetectedTextView.text = getString(
            R.string.threats_detected,
            preferenceManager.threatsDetected
        )

        val lastScanTime = preferenceManager.lastScanTime
        lastScanTextView.text = if (lastScanTime > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            getString(R.string.last_scan, dateFormat.format(Date(lastScanTime)))
        } else {
            getString(R.string.last_scan, "Never")
        }
    }

    private fun startMonitoring() {
        if (!PermissionUtil.areAllPermissionsGranted(this)) {
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_SHORT).show()
            return
        }

        // Start the monitoring service
        val serviceIntent = Intent(this, CallMonitoringService::class.java).apply {
            action = CallMonitoringService.ACTION_START_MONITORING
        }
        startForegroundService(serviceIntent)

        preferenceManager.isMonitoringEnabled = true
        preferenceManager.lastScanTime = System.currentTimeMillis()

        Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopMonitoring() {
        // Stop the monitoring service
        val serviceIntent = Intent(this, CallMonitoringService::class.java).apply {
            action = CallMonitoringService.ACTION_STOP_MONITORING
        }
        startService(serviceIntent)

        preferenceManager.isMonitoringEnabled = false

        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun requestPermissions() {
        val missingPermissions = PermissionUtil.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun showPermissionExplanation() {
        val missingPermissions = PermissionUtil.getMissingPermissions(this)
        if (missingPermissions.isEmpty()) return

        val permissionList = missingPermissions.joinToString("\n") { permission ->
            "• ${PermissionUtil.getPermissionDisplayName(permission)}"
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permissions_required))
            .setMessage("${getString(R.string.permissions_message)}\n\nRequired permissions:\n$permissionList")
            .setPositiveButton(getString(R.string.grant_permissions)) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
