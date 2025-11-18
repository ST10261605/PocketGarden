package com.example.pocketgarden.utils

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // Below Android 12, exact alarms are granted by default
            true
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13, notification permission is granted by default
            true
        }
    }

    fun logPermissionsStatus(context: Context) {
        Log.d("PermissionHelper", "Exact Alarm Permission: ${hasExactAlarmPermission(context)}")
        Log.d("PermissionHelper", "Notification Permission: ${hasNotificationPermission(context)}")
    }
}