package com.vishingalert.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlertActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Set the Activity to full screen, keeping the screen on, and placing it over the lock screen.
        // This is the CRITICAL part that makes it intrusive.
        turnScreenOnAndShowOnLockScreen()

        // Use a simple layout to ensure maximum contrast and attention
        setContentView(R.layout.activity_alert)

        val warningTextView: TextView = findViewById(R.id.warning_text)
        val hangUpButton: Button = findViewById(R.id.hang_up_button)

        warningTextView.text = "!!! URGENT FRAUD ALERT !!!\nHang Up Immediately and DO NOT share data."

        Log.d("VISHING_GUARD", "AlertActivity launched. Screen takeover initiated.")

        // The only action the user should take is to hang up.
        hangUpButton.setOnClickListener {
            // Dismiss the alert
            finish()
            // Note: We cannot programmatically hang up the call without specific permissions
            // (e.g., MODIFY_PHONE_STATE, which is restricted) but encouraging the user to hang up is the goal.
        }
    }

    private fun turnScreenOnAndShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Android 8.1+ method
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // Deprecated method for older devices
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
