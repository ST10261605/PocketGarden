package com.example.pocketgarden.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pocketgarden.MainActivity
import com.example.pocketgarden.R
import kotlin.jvm.java

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_WATER = "water_reminders"
        const val CHANNEL_ID_FERTILIZER = "fertilizer_reminders"
        const val NOTIFICATION_ID_WATER = 1001
        const val NOTIFICATION_ID_FERTILIZER = 1002
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Water reminder channel
            val waterChannel = NotificationChannel(
                CHANNEL_ID_WATER,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to water your plants"
                enableVibration(true)
                setShowBadge(true)
            }

            // Fertilizer reminder channel
            val fertilizerChannel = NotificationChannel(
                CHANNEL_ID_FERTILIZER,
                "Fertilizer Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to fertilize your plants"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(waterChannel, fertilizerChannel))
        }
    }

    fun showWaterReminder(plantName: String?, reminderId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WATER)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Time to water your plant!")
            .setContentText("Your $plantName needs watering")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "Mark as Watered",
                createWateredAction(plantName, reminderId)
            )
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
    }

    fun showFertilizerReminder(plantName: String, reminderId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FERTILIZER)
            .setSmallIcon(R.drawable.ic_fertilizer)
            .setContentTitle("Time to fertilize your plant!")
            .setContentText("Your $plantName needs fertilizing")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "Mark as Fertilized",
                createFertilizedAction(plantName, reminderId)
            )
            .build()

        notificationManager.notify(reminderId.hashCode(), notification)
    }

    private fun createWateredAction(plantName: String?, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_MARK_WATERED"
            putExtra("plant_name", plantName)
            putExtra("reminder_id", reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createFertilizedAction(plantName: String, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_MARK_FERTILIZED"
            putExtra("plant_name", plantName)
            putExtra("reminder_id", reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}