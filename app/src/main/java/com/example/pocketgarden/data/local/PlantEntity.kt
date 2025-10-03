package com.example.pocketgarden.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val remoteId: String? = null, // Plant.id response id (if available)
    val name: String? = null, // user-confirmed or name that API fetches from database
    val probability: Double? = null,
    val commonNames: String? = null,
    val imageUri: String, // content:// or file://
    val addedAt: Long = System.currentTimeMillis(),
    val lastWateredAt: Long? = null,
    val lastFertilizedAt: Long? = null,
    val watered: Boolean = false,
    val fertilized: Boolean = false,
    val synced: Boolean = false, // whether identification completed & saved
    val status: String = "PENDING" // PENDING, IDENTIFIED, FAILED
)