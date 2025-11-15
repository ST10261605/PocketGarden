package com.example.pocketgarden.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity): Long

    @Delete
    suspend fun delete(plant: PlantEntity)

    @Update
    suspend fun update(plant: PlantEntity)

    @Query("SELECT * FROM plants ORDER BY addedAt DESC")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE synced = 0")
    suspend fun getPendingPlants(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE localId = :id")
    suspend fun getPlantById(id: Long): PlantEntity?

    @Query("SELECT * FROM plants WHERE syncStatus != 'SYNCED' ORDER BY localId ASC")
    suspend fun getUnsyncedPlants(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE mongoDBId = :mongoDBId")
    suspend fun getPlantByMongoDBId(mongoDBId: String): PlantEntity?

    @Query("UPDATE plants SET syncStatus = :syncStatus, lastSyncAttempt = :timestamp WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: Long, syncStatus: SyncStatus, timestamp: Long)

    @Query("UPDATE plants SET syncStatus = :syncStatus, syncError = :error, lastSyncAttempt = :timestamp WHERE localId = :localId")
    suspend fun updateSyncStatusWithError(localId: Long, syncStatus: SyncStatus, error: String?, timestamp: Long)
}