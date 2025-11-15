package com.example.pocketgarden

import android.util.Log
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.network.FirestorePlant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncRepository {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val PLANTS_COLLECTION = "plants"
    }

    suspend fun syncPlantToFirestore(plant: PlantEntity): Boolean {
        return try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: "anonymous"
            val deviceId = getDeviceId()

            val firestorePlant = FirestorePlant.fromPlantEntity(plant, userId, deviceId)

            val documentReference = if (plant.firestoreId != null) {
                // Update existing plant
                firestore.collection(PLANTS_COLLECTION).document(plant.firestoreId)
            } else {
                // Create new plant
                firestore.collection(PLANTS_COLLECTION).document()
            }

            // Set with merge to update existing or create new
            documentReference.set(firestorePlant, SetOptions.merge()).await()

            Log.d("FirestoreSync", "Successfully synced plant ${plant.localId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to sync plant to Firestore: ${e.message}")
            false
        }
    }

    suspend fun deletePlantFromFirestore(firestoreId: String): Boolean {
        return try {
            firestore.collection(PLANTS_COLLECTION)
                .document(firestoreId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to delete plant from Firestore: ${e.message}")
            false
        }
    }

    suspend fun fetchPlantsFromFirestore(): List<FirestorePlant> {
        return try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: "anonymous"

            val querySnapshot = firestore.collection(PLANTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            querySnapshot.toObjects(FirestorePlant::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to fetch plants from Firestore: ${e.message}")
            emptyList()
        }
    }

    private fun getDeviceId(): String {
        // You can use Android ID or generate a UUID for the device
        return UUID.randomUUID().toString()
    }

    // Enable offline persistence (call this in your Application class)
    fun enableOfflinePersistence() {
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
}