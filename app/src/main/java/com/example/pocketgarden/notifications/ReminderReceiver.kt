package com.example.pocketgarden.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pocketgarden.data.local.ReminderType
import com.example.pocketgarden.repository.PlantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all reminders on boot
                scope.launch {
                    rescheduleAllReminders(context)
                }
            }
            "ACTION_MARK_WATERED" -> {
                val plantName = intent.getStringExtra("plant_name") ?: return
                val reminderId = intent.getStringExtra("reminder_id") ?: return
                scope.launch {
                    markPlantAsWatered(context, plantName, reminderId)
                }
            }
            "ACTION_MARK_FERTILIZED" -> {
                val plantName = intent.getStringExtra("plant_name") ?: return
                val reminderId = intent.getStringExtra("reminder_id") ?: return
                scope.launch {
                    markPlantAsFertilized(context, plantName, reminderId)
                }
            }
            else -> {
                // Handle regular reminder
                val reminderId = intent.getStringExtra("reminder_id") ?: return
                val plantName = intent.getStringExtra("plant_name") ?: return
                val reminderType = intent.getSerializableExtra("reminder_type") as? ReminderType ?: return

                scope.launch {
                    showReminderNotification(context, plantName, reminderId, reminderType)
                }
            }
        }
    }

    private suspend fun showReminderNotification(
        context: Context,
        plantName: String,
        reminderId: String,
        reminderType: ReminderType
    ) {
        val notificationHelper = NotificationHelper(context)

        when (reminderType) {
            ReminderType.WATERING -> notificationHelper.showWaterReminder(plantName, reminderId)
            ReminderType.FERTILIZING -> notificationHelper.showFertilizerReminder(plantName, reminderId)
            else -> {
                // Handle other reminder types
            }
        }
    }

    private suspend fun markPlantAsWatered(context: Context, plantName: String, reminderId: String) {
        try {
            val repository = PlantRepository.getInstance(context)
            // Update plant's last watered date and reschedule reminder
            // You'll need to implement this method in your repository
            Log.d("ReminderReceiver", "Marked $plantName as watered")
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error marking plant as watered", e)
        }
    }

    private suspend fun markPlantAsFertilized(context: Context, plantName: String, reminderId: String) {
        try {
            val repository = PlantRepository.getInstance(context)
            // Update plant's last fertilized date and reschedule reminder
            Log.d("ReminderReceiver", "Marked $plantName as fertilized")
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error marking plant as fertilized", e)
        }
    }

    private suspend fun rescheduleAllReminders(context: Context) {
        try {
            val repository = PlantRepository.getInstance(context)
            repository.rescheduleAllReminders()
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error rescheduling reminders", e)
        }
    }
}