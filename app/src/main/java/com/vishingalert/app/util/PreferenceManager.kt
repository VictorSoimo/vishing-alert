package com.vishingalert.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for managing app preferences
 */
class PreferenceManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "vishing_alert_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_CALLS_MONITORED = "calls_monitored"
        private const val KEY_THREATS_DETECTED = "threats_detected"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING_ENABLED, value).apply()

    var callsMonitored: Int
        get() = prefs.getInt(KEY_CALLS_MONITORED, 0)
        set(value) = prefs.edit().putInt(KEY_CALLS_MONITORED, value).apply()

    var threatsDetected: Int
        get() = prefs.getInt(KEY_THREATS_DETECTED, 0)
        set(value) = prefs.edit().putInt(KEY_THREATS_DETECTED, value).apply()

    var lastScanTime: Long
        get() = prefs.getLong(KEY_LAST_SCAN_TIME, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_SCAN_TIME, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    fun incrementCallsMonitored() {
        callsMonitored++
    }

    fun incrementThreatsDetected() {
        threatsDetected++
    }

    fun clearStatistics() {
        prefs.edit()
            .putInt(KEY_CALLS_MONITORED, 0)
            .putInt(KEY_THREATS_DETECTED, 0)
            .putLong(KEY_LAST_SCAN_TIME, 0)
            .apply()
    }
}
