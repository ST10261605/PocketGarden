package com.example.pocketgarden.network

import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.data.local.SyncStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp
import java.util.Date

data class FirestorePlant(
    @DocumentId
    val id: String = "",
    val localId: Long = 0,
    val imageUri: String = "",
    val name: String = "",
    val probability: Double? = null,
    val commonNames: String? = null,
    val remoteId: String? = null,
    val status: String = "IDENTIFIED",
    val userId: String = "",
    val deviceId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "SYNCED" // SYNCED, PENDING, FAILED
) {
    companion object {
        fun fromPlantEntity(entity: PlantEntity, userId: String, deviceId: String): FirestorePlant {
            return FirestorePlant(
                localId = entity.localId,
                imageUri = entity.imageUri,
                name = entity.name ?: "Unknown Plant",
                probability = entity.probability,
                commonNames = entity.commonNames,
                remoteId = entity.remoteId,
                status = entity.status,
                userId = userId,
                deviceId = deviceId,
                createdAt = Timestamp(Date(entity.createdAt)),
                updatedAt = Timestamp(Date(entity.updatedAt)),
                syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) "SYNCED" else "PENDING"
            )
        }
    }
}