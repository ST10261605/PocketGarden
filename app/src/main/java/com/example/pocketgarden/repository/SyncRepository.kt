package com.example.pocketgarden.repository

import android.content.Context
import android.util.Log
import com.example.pocketgarden.data.local.FirestorePlantNote
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantNote
import com.example.pocketgarden.data.local.PlantNoteDAO
import com.example.pocketgarden.network.NetworkHelper
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncRepository(
    private val plantNoteDao: PlantNoteDAO,
    private val plantDao: PlantDAO,
    private val networkHelper: NetworkHelper,
    private val context: Context
) {
    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Start monitoring network changes if API level is sufficient
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            networkHelper.startNetworkMonitoring()
        }

        // Auto-sync when network becomes available
        scope.launch {
            networkHelper.networkStatus.collect { isConnected ->
                if (isConnected) {
                    Log.d("SyncRepository", "Network available, attempting to sync notes")
                    syncNotes()
                }
            }
        }
    }

    suspend fun syncNotes() {
        if (!networkHelper.isOnline()) {
            Log.d("SyncRepository", "Device is offline, skipping sync")
            return
        }

        try {
            val unsyncedNotes = plantNoteDao.getUnsyncedNotes()
            Log.d("SyncRepository", "Found ${unsyncedNotes.size} unsynced notes")

            unsyncedNotes.forEach { note ->
                syncNoteToFirestore(note)
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error syncing notes: ${e.message}")
        }
    }

    private suspend fun syncNoteToFirestore(note: PlantNote) {
        try {
            // Get plant to find its Firestore ID
            val plant = plantDao.getPlantById(note.plantLocalId)
            if (plant?.firestoreId == null) {
                Log.e("SyncRepository", "Plant Firestore ID not found for local ID: ${note.plantLocalId}")
                return
            }

            val firestoreNote = FirestorePlantNote(
                id = note.id,
                plantFirestoreId = plant.firestoreId,
                content = note.content,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt
            )

            // Using the local ID as Firestore document ID for consistency
            firestore.collection("plant_notes")
                .document(note.id)
                .set(firestoreNote)
                .await()

            // Mark as synced in room DB
            plantNoteDao.markNoteAsSynced(note.id, note.id)
            Log.d("SyncRepository", "Successfully synced note: ${note.id}")

        } catch (e: Exception) {
            Log.e("SyncRepository", "Error syncing note ${note.id}: ${e.message}")
            throw e
        }
    }

    suspend fun deleteNoteFromFirestore(note: PlantNote) {
        if (!networkHelper.isOnline()) {
            Log.d("SyncRepository", "Device offline, cannot delete from Firestore")
            return
        }

        try {
            if (note.firestoreId != null) {
                firestore.collection("plant_notes")
                    .document(note.firestoreId)
                    .delete()
                    .await()
                Log.d("SyncRepository", "Successfully deleted note from Firestore: ${note.firestoreId}")
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error deleting note from Firestore: ${e.message}")
        }
    }

    fun getNetworkStatus() = networkHelper.networkStatus
}