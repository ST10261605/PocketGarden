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

}