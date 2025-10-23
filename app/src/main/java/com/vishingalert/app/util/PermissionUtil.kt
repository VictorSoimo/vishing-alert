package com.vishingalert.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtil {

    /**
     * All permissions required by the app
     */
    val REQUIRED_PERMISSIONS = buildList {
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }.toTypedArray()

    /**
     * Checks if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
               PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all required permissions are granted
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            isPermissionGranted(context, permission)
        }
    }

    /**
     * Gets list of permissions that are not granted
     */
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            !isPermissionGranted(context, permission)
        }
    }

    /**
     * Gets permission names in a user-friendly format
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_PHONE_STATE -> "Phone State"
            Manifest.permission.READ_CALL_LOG -> "Call Log"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.RECEIVE_SMS -> "Receive SMS"
            Manifest.permission.READ_SMS -> "Read SMS"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.ANSWER_PHONE_CALLS -> "Answer Calls"
            else -> permission
        }
    }
}
