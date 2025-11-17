package com.example.pocketgarden.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantNoteDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: PlantNote)

    @Update
    suspend fun update(note: PlantNote)

    @Delete
    suspend fun delete(note: PlantNote)

    @Query("SELECT * FROM plant_notes WHERE plantLocalId = :plantLocalId ORDER BY createdAt DESC")
    fun getNotesForPlant(plantLocalId: Long): Flow<List<PlantNote>>

    @Query("DELETE FROM plant_notes WHERE plantLocalId = :plantLocalId")
    suspend fun deleteNotesForPlant(plantLocalId: Long)

    @Query("SELECT COUNT(*) FROM plant_notes WHERE plantLocalId = :plantLocalId")
    suspend fun getNoteCountForPlant(plantLocalId: Long): Int
}