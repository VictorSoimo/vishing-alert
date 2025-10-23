package com.vishingalert.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Broadcast receiver for monitoring phone call states
 */
class PhoneCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneCallReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var callStartTime = 0L
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d(TAG, "Phone ringing from: $phoneNumber")
                onIncomingCall(context, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.CALL_STATE_OFFHOOK) {
                    Log.d(TAG, "Call answered: $phoneNumber")
                    callStartTime = System.currentTimeMillis()
                    onCallAnswered(context, phoneNumber)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    val duration = System.currentTimeMillis() - callStartTime
                    Log.d(TAG, "Call ended: $phoneNumber, duration: ${duration}ms")
                    onCallEnded(context, phoneNumber, duration)
                }
            }
        }

        lastState = when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            else -> TelephonyManager.CALL_STATE_IDLE
        }
    }

    private fun onIncomingCall(context: Context, phoneNumber: String) {
        // Log incoming call for statistics
        Log.d(TAG, "Incoming call detected from $phoneNumber")
    }

    private fun onCallAnswered(context: Context, phoneNumber: String) {
        // Start monitoring/recording when call is answered
        Log.d(TAG, "Call answered, starting analysis for $phoneNumber")
        
        // Note: Actual audio capture would require system-level permissions
        // This is where you would trigger the speech-to-text analysis
    }

    private fun onCallEnded(context: Context, phoneNumber: String, duration: Long) {
        // Process completed call
        Log.d(TAG, "Call ended from $phoneNumber after ${duration}ms")
    }
}
