package com.example.pocketgarden.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "plant_reminders")
data class PlantReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val plantLocalId: Long,
    val plantName: String?,
    val reminderType: ReminderType,
    val reminderTime: Long, // timestamp
    val repeatInterval: Long, // in milliseconds
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    WATERING, FERTILIZING, PRUNING, OTHER
}

data class ReminderSchedule(
    val frequencyDays: Int,
    val timeOfDay: String, // e.g., "09:00"
    val startDate: Long // timestamp
)