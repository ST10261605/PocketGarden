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
    val status: String = "PENDING", // PENDING, IDENTIFIED, FAILED

    // Firestore sync fields
    val firestoreId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Long = 0,
    val syncError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val userId: String? = null
)
enum class SyncStatus {
    PENDING, SYNCING, SYNCED, FAILED
}