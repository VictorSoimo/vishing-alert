package com.vishingalert.app.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Call Screening Service for Android 10+
 * Intercepts incoming calls for analysis
 */
@RequiresApi(Build.VERSION_CODES.Q)
class VishingCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "VishingCallScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
        Log.d(TAG, "Screening call from: $phoneNumber")

        // For now, allow all calls but log them
        // In the future, could integrate with a blacklist or real-time risk assessment
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)

        // Notify the monitoring service about the call
        // This would trigger audio capture and analysis
        Log.d(TAG, "Call allowed for monitoring: $phoneNumber")
    }
}
