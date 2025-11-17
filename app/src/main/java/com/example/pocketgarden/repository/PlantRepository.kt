package com.example.pocketgarden.repository

import android.content.Context
import android.content.SyncResult
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.pocketgarden.AppDatabase
import com.example.pocketgarden.FirestoreSyncRepository
import com.example.pocketgarden.data.local.PlantDAO
import com.example.pocketgarden.data.local.PlantEntity
import com.example.pocketgarden.data.local.PlantNote
import com.example.pocketgarden.data.local.PlantNoteDAO
import com.example.pocketgarden.data.local.SyncStatus
import com.example.pocketgarden.network.PlantIdApi
import com.example.pocketgarden.network.IdentificationRequestV3
import com.example.pocketgarden.network.IdentificationResponse // Add this import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.text.insert

class PlantRepository(
    private val api: PlantIdApi,
    private val plantDao: PlantDAO,
    val apiKeyProvider: ApiKeyProvider, // interface to get API key or proxy URL
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val connectivityManager: ConnectivityManager,
    private val plantNoteDao: PlantNoteDAO
) {

    sealed class SyncResult {
        object NO_NETWORK : SyncResult()
        data class SUCCESS(val successCount: Int, val failureCount: Int) : SyncResult()
        data class ERROR(val message: String) : SyncResult()
    }

    //function to add a plant offline, the pending images are only sent for identification once user is back online
    //adding plant to roomdb until user returns online
    suspend fun addPlantOffline(imageUri: String): Long {
        val entity = PlantEntity(imageUri = imageUri, synced = false, status = "PENDING")
        return plantDao.insert(entity)
    }

    suspend fun savePlant(plant: PlantEntity): Long {
        val localId = plantDao.insert(plant)

        // Try to sync to Firestore immediately if online
        if (isOnline()) {
            syncPlantToFirestore(localId)
        }

        return localId
    }

    suspend fun deletePlant(plant: PlantEntity) {
        // If plant is synced to Firestore, mark for deletion
        if (plant.firestoreId != null) {
            val updatedPlant = plant.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            )
            plantDao.update(updatedPlant)

            // Try to delete from Firestore immediately if online
            if (isOnline()) {
                syncDeletions()
            }
        } else {
            // If never synced, just delete locally
            plantDao.delete(plant)
        }
    }

    //plant note functionality -- for offline sync feature
    suspend fun addPlantNote(plantLocalId: Long, content: String) {
        val note = PlantNote(
            plantLocalId = plantLocalId,
            content = content
        )
        plantNoteDao.insert(note)
    }

    fun getPlantNotes(plantLocalId: Long): Flow<List<PlantNote>> {
        return plantNoteDao.getNotesForPlant(plantLocalId)
    }

    suspend fun deletePlantNote(note: PlantNote) {
        plantNoteDao.delete(note)
    }

    suspend fun updatePlantNote(note: PlantNote) {
        plantNoteDao.update(note)
    }

    suspend fun getNoteCountForPlant(plantLocalId: Long): Int {
        return plantNoteDao.getNoteCountForPlant(plantLocalId)
    }

    suspend fun syncPendingPlants(): SyncResult {
        return try {
            if (!isOnline()) {
                return SyncResult.NO_NETWORK
            }

            val unsyncedPlants = plantDao.getUnsyncedPlants()
            var successCount = 0
            var failureCount = 0

            unsyncedPlants.forEach { plant ->
                // Update sync status to SYNCING
                plantDao.updateSyncStatus(plant.localId, SyncStatus.SYNCING, System.currentTimeMillis())

                val syncSuccess = firestoreSyncRepository.syncPlantToFirestore(plant)

                if (syncSuccess) {
                    // For Firestore, need to handle the document ID
                    // might need to fetch the actual ID
                    val updatedPlant = plant.copy(
                        syncStatus = SyncStatus.SYNCED,
                        firestoreId = plant.firestoreId ?: "firestore_${plant.localId}",
                        updatedAt = System.currentTimeMillis()
                    )
                    plantDao.update(updatedPlant)
                    successCount++
                } else {
                    plantDao.updateSyncStatusWithError(
                        plant.localId,
                        SyncStatus.FAILED,
                        "Sync failed",
                        System.currentTimeMillis()
                    )
                    failureCount++
                }
            }

            // Sync deletions
            syncDeletions()

            SyncResult.SUCCESS(successCount, failureCount)
        } catch (e: Exception) {
            Log.e("PlantRepository", "Error syncing pending plants: ${e.message}")
            SyncResult.ERROR(e.message ?: "Unknown error")
        }
    }

    private suspend fun syncPlantToFirestore(localId: Long) {
        val plant = plantDao.getPlantById(localId) ?: return
        plantDao.updateSyncStatus(localId, SyncStatus.SYNCING, System.currentTimeMillis())

        val syncSuccess = firestoreSyncRepository.syncPlantToFirestore(plant)

        if (syncSuccess) {
            val updatedPlant = plant.copy(
                syncStatus = SyncStatus.SYNCED,
                firestoreId = plant.firestoreId ?: "firestore_${plant.localId}",
                updatedAt = System.currentTimeMillis()
            )
            plantDao.update(updatedPlant)
        } else {
            plantDao.updateSyncStatusWithError(
                localId,
                SyncStatus.FAILED,
                "Sync failed",
                System.currentTimeMillis()
            )
        }
    }

    private suspend fun syncDeletions() {
        // Get plants marked for deletion and sync them
        val plantsToDelete = plantDao.getPlantsMarkedForDeletion()
        plantsToDelete.forEach { plant ->
            plant.firestoreId?.let { firestoreId ->
                val deleteSuccess = firestoreSyncRepository.deletePlantFromFirestore(firestoreId)
                if (deleteSuccess) {
                    plantDao.delete(plant)
                }
            }
        }
    }

    private fun isOnline(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    suspend fun getAllPlantsFlow() = plantDao.getAllPlants()

    companion object {
        @Volatile private var INSTANCE: PlantRepository? = null

        fun getInstance(context: Context): PlantRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val dao = db.plantDao()
                val api = PlantIdApi.create()
                val plantNoteDao = db.plantNoteDao()

                // Get ConnectivityManager
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // Create FirestoreSyncRepository
                val firestoreSyncRepository = FirestoreSyncRepository().apply {
                    enableOfflinePersistence()
                }

                val provider = object : ApiKeyProvider {
                    override fun getApiKey(): String = "mRnpO239bpQY3EcOGlxTgQ9GfXl2Krg6Xqqg4WhDkzzXEwSvlX"

                    override suspend fun readUriAsBase64(uriString: String): String {
                        return withContext(Dispatchers.IO) {
                            try {
                                Log.d("ApiKeyProvider", "Processing URI: $uriString")

                                val uri = Uri.parse(uriString)
                                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

                                if (inputStream == null) {
                                    Log.e("ApiKeyProvider", "Could not open input stream for URI: $uriString")
                                    return@withContext ""
                                }

                                // Read the raw bytes
                                val rawBytes = inputStream.readBytes()
                                inputStream.close()

                                // Convert to pure Base64 without data URL prefix
                                val base64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                                Log.d("ApiKeyProvider", "Pure base64 length: ${base64.length}")

                                return@withContext base64

                            } catch (e: Exception) {
                                Log.e("ApiKeyProvider", "Error reading URI as Base64: ${e.message}", e)
                                ""
                            }
                        }
                    }
                }

                PlantRepository(
                    api = api,
                    plantDao = dao,
                    apiKeyProvider = provider,
                    firestoreSyncRepository = firestoreSyncRepository,
                    connectivityManager = connectivityManager,
                    plantNoteDao = plantNoteDao
                ).also { INSTANCE = it }
            }
        }
    }

    suspend fun identifyPlantFromBitmapBase64V3(base64: String): IdentificationResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("PlantRepository", "Starting plant identification...")
                Log.d("PlantRepository", "Base64 first 50 chars: ${base64.take(50)}")
                Log.d("PlantRepository", "Base64 length: ${base64.length}")

                // Build the request
                val request = IdentificationRequestV3(
                    images = listOf(base64),
                    modifiers = listOf("similar_images"),
                    organs = listOf("leaf"),
                    latitude = 0.0,
                    longitude = 0.0,
                    lang = "en"
                )

                // Call the Plant.id API
                val resp = api.identify(
                    apiKey = apiKeyProvider.getApiKey(),
                    request = request
                )

                Log.d("PlantRepository", "Response code: ${resp.code()}")
                Log.d("PlantRepository", "Response isSuccessful: ${resp.isSuccessful}")

                if (resp.isSuccessful) {
                    val responseBody = resp.body()
                    Log.d("PlantRepository", "Full response: $responseBody")

                    // Debug: Log the raw response to see actual structure
                    val rawResponse = resp.raw().toString()
                    Log.d("PlantRepository", "Raw response: $rawResponse")

                    // Handle different response structures
                    val suggestions = when {
                        // v3 structure: result -> classification -> suggestions
                        responseBody?.result?.classification?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v3 structure")
                            responseBody.result.classification.suggestions
                        }
                        // v2 structure: direct suggestions field
                        responseBody?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v2 structure")
                            responseBody.suggestions
                        }
                        // v3 alternative: result -> suggestions
                        responseBody?.result?.suggestions != null -> {
                            Log.d("PlantRepository", "Using v3 alternative structure")
                            responseBody.result.suggestions
                        }
                        else -> {
                            Log.d("PlantRepository", "No suggestions found in any structure")
                            emptyList()
                        }
                    }

                    Log.d("PlantRepository", "Found ${suggestions.size} suggestions")

                    if (suggestions.isEmpty()) {
                        Log.d("PlantRepository", "No plant suggestions found")
                        return@withContext IdentificationResult.Success(responseBody)
                    }

                    IdentificationResult.Success(responseBody)
                } else {
                    val errorBody = resp.errorBody()?.string()
                    Log.e("PlantRepository", "API Error: ${resp.code()} - $errorBody")
                    IdentificationResult.Error(resp.code(), errorBody ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("PlantRepository", "Exception during identification: ${e.message}", e)
                IdentificationResult.Error(-1, e.localizedMessage ?: "Exception occurred")
            }
        }
    }
    }

sealed class IdentificationResult {
    data class Success(val response: IdentificationResponse?): IdentificationResult() // Fixed: removed package prefix
    data class Error(val code: Int, val message: String): IdentificationResult()
}

// small interface to provide key & helper to read URIs (so repository stays testable)
interface ApiKeyProvider {
    fun getApiKey(): String
    suspend fun readUriAsBase64(uriString: String): String
}
