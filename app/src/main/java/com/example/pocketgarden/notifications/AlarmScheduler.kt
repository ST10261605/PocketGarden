package com.example.pocketgarden.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pocketgarden.data.local.PlantReminder
import com.example.pocketgarden.data.local.ReminderType
import com.example.pocketgarden.utils.PermissionHelper

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: PlantReminder): Boolean {
        return try {
            // Check permissions first
            if (!PermissionHelper.hasExactAlarmPermission(context)) {
                Log.e("AlarmScheduler", "Missing exact alarm permission")
                return false
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("plant_name", reminder.plantName)
                putExtra("reminder_type", reminder.reminderType)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule the alarm based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.reminderTime,
                    pendingIntent
                )
            }

            Log.d("AlarmScheduler", "Successfully scheduled reminder for ${reminder.plantName} at ${reminder.reminderTime}")
            true
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling reminder: ${e.message}")
            false
        }
    }

    fun cancelReminder(reminderId: String) {
        try {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d("AlarmScheduler", "Cancelled reminder: $reminderId")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error cancelling reminder: ${e.message}")
        }
    }

    fun rescheduleReminder(reminder: PlantReminder): Boolean {
        cancelReminder(reminder.id)
        return if (reminder.isEnabled) {
            scheduleReminder(reminder)
        } else {
            true
        }
    }
}