package com.example.pocketgarden.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantReminderDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: PlantReminder)

    @Update
    suspend fun update(reminder: PlantReminder)

    @Delete
    suspend fun delete(reminder: PlantReminder)

    @Query("SELECT * FROM plant_reminders WHERE plantLocalId = :plantLocalId AND reminderType = :reminderType")
    suspend fun getReminderForPlant(plantLocalId: Long, reminderType: ReminderType): PlantReminder?

    @Query("SELECT * FROM plant_reminders WHERE plantLocalId = :plantLocalId")
    fun getRemindersForPlant(plantLocalId: Long): Flow<List<PlantReminder>>

    @Query("SELECT * FROM plant_reminders WHERE isEnabled = 1")
    suspend fun getActiveReminders(): List<PlantReminder>

    @Query("SELECT * FROM plant_reminders WHERE reminderTime <= :time AND isEnabled = 1")
    suspend fun getDueReminders(time: Long): List<PlantReminder>

    @Query("UPDATE plant_reminders SET isEnabled = :enabled WHERE id = :reminderId")
    suspend fun setReminderEnabled(reminderId: String, enabled: Boolean)

    @Query("DELETE FROM plant_reminders WHERE plantLocalId = :plantLocalId")
    suspend fun deleteRemindersForPlant(plantLocalId: Long)
}